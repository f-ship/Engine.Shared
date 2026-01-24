package ship.f.engine.shared.zone

import ship.f.engine.shared.core.ScopedEvent
import ship.f.engine.shared.core.ScopedEvent.*
import ship.f.engine.shared.sdui2.SDUIInput2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2
import ship.f.engine.shared.utils.serverdrivenui2.state.RefState2
import ship.f.engine.shared.zone.Zone.RequestResult.Success
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class Zone<D : DomainEvent6, O : Any?> (val update: KClass<out D>) {
    abstract val name: String
    /**
     * Called by the zone when the backing domain object is updated
     */
    abstract fun create(update: D): SDUIInput2

    /**
     * Called by the zone after create has either been ran or skipped
     */
    abstract fun update(input: ViewRequest6, sduiInput2: SDUIInput2): SDUIInput2

    /**
     * Called by the zone to another zone, it's the callee zones responsibility to add the requests to be shipped out
     */
    abstract fun promise(input: ViewRequest6): List<RefState2>

    /**
     * handleRequest should return a deterministic hash to represent the request
     */
    abstract fun handleRequest(input: ViewRequest6): RequestHash

    lateinit var send: (List<SDUIInput2>, String) -> Boolean
    lateinit var publish: (ViewRequest6) -> Unit

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
            is RequestResult.Pending -> Unit
            is Success<*> -> {
                send(cachedResult.output.map{ it.sdui }, hash.requesterId)
            }
            is RequestResult.Failed, null -> {
                val nonCachedDomains = hash.domainIds.filterNot { domains.containsKey(it) }
                if (nonCachedDomains.isNotEmpty() && update != StaticVoid6::class) {
                    publish(input)
                } else {
                    if (update == StaticVoid6::class) {
                        nonCachedDomains.forEach { domainId ->
                            val domain: DomainEvent6 = StaticVoid6(input)
                            val sdui = create(domain as D)
                            domains[domainId] = SduiDomain(domain, sdui)
                        }
                    }
                    requests[hash.requestId] = Success(output = hash.domainIds.mapNotNull { domains[it] })
                }

                val cachedDomains = hash.domainIds.mapNotNull { domains[it] }
                send(cachedDomains.map { update(input, it.sdui) }, hash.requesterId)
            }
        }
    }

    fun augmentRequest(input: ViewRequest6, domain: D) {
        val hash = handleRequest(input)
        val sdui = create(domain)
        val update = update(input, sdui)
        send(listOf(update), hash.requesterId)

        if (hash.domainIds.filterNot { domains.containsKey(it) }.isEmpty()) {
            requests[hash.requestId] = Success(output = hash.domainIds.mapNotNull { domains[it] })
        }
    }

    val domains = mutableMapOf<String, SduiDomain<D>>()
    val requests = mutableMapOf<String, RequestResult>() // <---- When this is updated
    val subscribedRequests = mutableMapOf<String, List<String>>() // <---- This is checked to see if anyone is waiting on that request if computed from scratch

    data class RequestHash(val requestId: String, val domainIds: List<String>, val requesterId: String)

    data class SduiDomain<D : ScopedEvent>(
        val domain: D,
        val sdui: SDUIInput2
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
        inline fun <reified D : DomainEvent6, reified O : Any?> create(
            name: String,
            crossinline create: (update: D) -> SDUIInput2,
            crossinline update: ((input: ViewRequest6, sduiInput2: SDUIInput2) -> SDUIInput2) = { _, sduiInput2 -> sduiInput2 },
            crossinline promise: (input: ViewRequest6) -> List<RefState2> =  { listOf(RefState2(id = Id2.StateId2(name)))},
            crossinline handleRequest: (input: ViewRequest6) -> RequestHash = { RequestHash(requestId = name, domainIds = listOf(), requesterId = it.requesterId) }
        ) = object : Zone<D, O>(D::class) {
            override val name = name
            override fun create(update: D) = create(update)
            override fun update(input: ViewRequest6, sduiInput2: SDUIInput2) = update(input, sduiInput2)
            override fun promise(input: ViewRequest6) = promise(input)
            override fun handleRequest(input: ViewRequest6) = handleRequest(input)
        }
    }
}