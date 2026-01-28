package ship.f.engine.shared.zone

import ship.f.engine.shared.core.ScopedEvent
import ship.f.engine.shared.core.ScopedEvent.*
import ship.f.engine.shared.sdui2.SDUIInput2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2
import ship.f.engine.shared.utils.serverdrivenui2.ext.sduiLog
import ship.f.engine.shared.utils.serverdrivenui2.state.RefState2
import ship.f.engine.shared.zone.Zone.RequestResult.Success
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class Zone<D : DomainEvent6, C : DomainEvent6> (val update: KClass<D>) {
    abstract val name: String
    /**
     * Called by the zone when the backing domain object is updated
     */
    abstract fun create(update: D): SDUIInput2

    /**
     * Called by the zone after create has either been ran or skipped
     */
    abstract fun update(input: ViewRequest6, sduiInput2: SDUIInput2, domain: D): SDUIInput2

    /**
     * Called by the zone to another zone, it's the callee zones responsibility to add the requests to be shipped out
     */
    protected abstract fun getPromise(input: ViewRequest6, multiDomain: C): List<Pair<RefState2, ScopedEvent?>>

    /**
     * handleRequest should return a deterministic hash to represent the request
     */
    abstract fun handleRequest(input: ViewRequest6): RequestHash

    /**
     * used to fire a request for the domain to update
     */
    abstract fun getDomain(domainId: String): D

    fun fufillPromise(input: ViewRequest6, multiDomain: C) = getPromise(input, multiDomain).map { (refState, domainEvent) ->
        // TODO we want to be checking if we already have the domain cached locally
        // TODO We need to improve engine so that we can have a shared cache, or so that subpubs can borrow caches
        // TODO a derived cache would be really good
        domainEvent?.let { publishScopedEvent(it) }
        refState
    }

    lateinit var send: (List<SDUIInput2>, String) -> Boolean
    lateinit var send2: (List<SduiDomain<*>>, List<String>, ViewRequest6) -> Unit
    lateinit var publish: (ViewRequest6) -> Unit
    lateinit var publishScopedEvent: (ScopedEvent) -> Unit

    /**
     * Called by the user as a request, if the request is a cache miss then it will be sent so the domain can update
     * 1) If requests hit the return immediately
     * 2) If requests miss but domains hits then run update
     * 3) if request and domain miss then publish the I
     */
    fun request(input: ViewRequest6) {
        val hash = handleRequest(input)

        val cachedResult = requests[hash.requestId]

        val requesterIds = subscribedRequests[hash.requestId] ?: listOf()
        subscribedRequests[hash.requestId] = (requesterIds + hash.domainIds).distinct()

        when(cachedResult) {
            is RequestResult.Pending -> sduiLog("RequestResult.Pending for $hash", tag = "ZoneGraph2 > request")
            is Success<*> -> {
                sduiLog("RequestResult.Success for $hash", tag = "ZoneGraph2 > request")
                send2(cachedResult.output, hash.domainIds, input)
            }
            is RequestResult.Failed, null -> {
                sduiLog("RequestResult.Failed or null for $hash", tag = "ZoneGraph2 > request")
                val nonCachedDomains = hash.domainIds.filterNot { sduiDomains.containsKey(it) }
                if (nonCachedDomains.isNotEmpty() && update != StaticVoid6::class) {
                    // TODO I think I still need to change the request to pending at a certain point here
                    val domains = nonCachedDomains.map { getDomain(it) }
                    domains.forEach { augmentRequest(input, it) }
                } else {
                    if (update == StaticVoid6::class) {
                        // TODO this must be a massive incorrect implementation
                        val domain: DomainEvent6 = StaticVoid6(input)
                        val sdui = create(domain as D)
                        sduiDomains[input.id.name] = SduiDomain(domain, sdui, input.id.name)
                    }
                    requests[hash.requestId] = Success(output = hash.domainIds.mapNotNull { sduiDomains[it] })
                }

                val cachedDomains = hash.domainIds.mapNotNull { sduiDomains[it] }
                send2(cachedDomains.map { it.copy(sdui = update(input, it.sdui, it.domain)) }, hash.domainIds, input)
            }
        }
    }

    fun augmentRequest(input: ViewRequest6, domain: DomainEvent6) {
        val hash = handleRequest(input)
        val sdui = create(domain as D)
        val update = update(input, sdui, domain)
        send2(listOf(SduiDomain(domain, update, hash.domainIds.firstOrNull().orEmpty()), ), hash.domainIds, input)

        if (hash.domainIds.filterNot { sduiDomains.containsKey(it) }.isEmpty()) {
            requests[hash.requestId] = Success(output = hash.domainIds.mapNotNull { sduiDomains[it] })
        }
    }

    val sduiDomains = mutableMapOf<String, SduiDomain<D>>()
    val requests = mutableMapOf<String, RequestResult>() // <---- When this is updated
    val subscribedRequests = mutableMapOf<String, List<String>>() // <---- This is checked to see if anyone is waiting on that request if computed from scratch

    data class RequestHash(val requestId: String, val domainIds: List<String>, val requesterId: String)

    data class SduiDomain<D : ScopedEvent>(
        val domain: D,
        val sdui: SDUIInput2,
        val domainHash: String,
    )

    sealed class RequestResult {
        data class Success<D : ScopedEvent>(val output: List<SduiDomain<D>>) : RequestResult()
        data object Pending : RequestResult()
        data object Failed : RequestResult() // TODO to do at some point
    }

    /**
     * In theory this could be a perfect way to register a streaming interest in requests that update
     * Need sessionDependency send to indicate if sending was successful or not so we can remove from pendingRequests
     */

    companion object {
        inline fun <reified D : DomainEvent6, reified C : DomainEvent6> create(
            name: String,
            crossinline create: (update: D) -> SDUIInput2,
            crossinline update: ((input: ViewRequest6, sduiInput2: SDUIInput2, domain: D) -> SDUIInput2) = { _, sduiInput2,_  -> sduiInput2 },
            crossinline getPromise: (input: ViewRequest6, multiDomain: C) -> List<Pair<RefState2, ScopedEvent?>> =  { _, _ -> listOf(Pair(RefState2(id = Id2.StateId2(name)), null))},
            crossinline handleRequest: (input: ViewRequest6) -> RequestHash = { RequestHash(requestId = name, domainIds = listOf(name), requesterId = it.requesterId) },
            crossinline getDomain: (domainId: String) -> D = { error("get domain is not implemented for $name") }
        ) = object : Zone<D,C>(D::class) {
            override val name = name
            override fun create(update: D) = create(update)
            override fun update(input: ViewRequest6, sduiInput2: SDUIInput2, domain: D) = update(input, sduiInput2, domain)
            override fun getPromise(input: ViewRequest6, multiDomain: C) = getPromise(input, multiDomain)
            override fun handleRequest(input: ViewRequest6) = handleRequest(input)
            override fun getDomain(domainId: String) = getDomain(domainId)
        }
    }
}