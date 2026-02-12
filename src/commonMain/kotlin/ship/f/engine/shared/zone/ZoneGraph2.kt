package ship.f.engine.shared.zone

import ship.f.engine.shared.core.ScopedEvent
import ship.f.engine.shared.core.ScopedEvent.*
import ship.f.engine.shared.core.ScopedEvent.StaticVoid6.Companion.toComputingViewRequest6
import ship.f.engine.shared.core.ScopedEvent.ViewRequest6.InitiatedViewRequest6
import ship.f.engine.shared.sdui2.SDUIInput2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2
import ship.f.engine.shared.utils.serverdrivenui2.ext.sduiLog
import ship.f.engine.shared.utils.serverdrivenui2.state.RefState2
import ship.f.engine.shared.zone.Zone.RequestResult.Success
import ship.f.engine.shared.zone.Zone.SduiDomain.*
import kotlin.reflect.KClass

@Suppress("UNCHECKED_CAST")
abstract class Zone<D : DomainEvent6>(val update: KClass<D>) {
    abstract val name: String
    lateinit var send: (List<SDUIInput2>, String) -> Boolean
    lateinit var send2: (sduis: List<ComputedSduiDomain<*>>, List<String>, ViewRequest6) -> Unit
    lateinit var publish: (DomainEvent6) -> Unit

    var counter = 0

    /**
     * Called by the zone when the backing domain object is updated
     */
    abstract fun create(update: D, requestId: String): SDUIInput2

    /**
     * Called by the zone after create has either been ran or skipped
     */
    abstract fun update(input: DomainViewRequest6, sduiInput2: SDUIInput2, domain: D): SDUIInput2

    /**
     * Called by the zone to another zone, it's the callee zones responsibility to add the requests to be shipped out
     */
    protected abstract fun getPromise(input: DomainViewRequest6): List<Pair<RefState2, DomainEvent6?>>

    /**
     * moving away from a hash based system to an input based system to enable easier flow of data
     */
    abstract fun initiateInput(input: UninitiatedViewRequest6): TrivialViewRequest6

    /**
     * requestDomain
     */
    abstract fun requestDomain(input: InitiatedViewRequest6, sduiDomainId: String): DomainEvent6

    // TODO I need to create the concept of synthetic requests that get built after real request to enable resending promises
    fun fulfillPromise(input: DomainViewRequest6) = getPromise(input) // TODO convert to a derivative View Request
        .map { (refState, domainEvent) ->
            sduiLog("Fulfilling promise for $refState with $counter", tag = "ZoneGraph2 > fulfillPromise")
            domainEvent?.let { publish(it) }
            refState
        }

    /**
     * Called by the user as a request, if the request is a cache miss then it will be sent so the domain can update
     * 1) If requests hit the return immediately
     * 2) If requests miss but domains hits then run update
     * 3) if request and domain miss then publish the I
     */
    fun request(input: ViewRequest6): List<RefState2> {
        val currentInput = when(input) {
            is UninitiatedViewRequest6 -> initiateInput(input)
            is TrivialViewRequest6 -> input
            is DomainViewRequest6 -> input
        }
        val refs = currentInput.domainIds.map { RefState2(id = Id2.StateId2(name = name, scope = it)) }

        // TODO This adds the current requester to the list of subscribers, it's not currently used
        val requesterIds = subscribedRequests[currentInput.requestId] ?: listOf()
        subscribedRequests[currentInput.requestId] = (requesterIds + currentInput.requesterId).distinct()

        when (val cachedResult = requests[currentInput.requestId]) {
            /**
             * If the request has been cached, we don't need to run update()
             * The hash should be different if update() is required to be run again
             */
            is Success<*> -> {
                sduiLog("RequestResult.Success for $currentInput", tag = "ZoneGraph2 > request")
                send2(cachedResult.output, currentInput.domainIds, currentInput) // In the future send to all subscribedRequests

                internalZoneMap.forEach {
                    it.value.requests[currentInput.requestId]?.let { requestResult ->
                        (requestResult as? Success<*>)?.let { success ->
                            if (it.key != name) {
                                send2(success.output, (success.input as? DomainViewRequest6)?.domainIds.orEmpty(), input)
                            }
                        }
                    }
                }
            }
            /**
             * If the request has not been cached, then we will always need to run create() to update the sduiDomain cache
             * And then run update() to update the request cache
             */
            is RequestResult.Failed, RequestResult.Pending, null -> {
                if (cachedResult is RequestResult.Pending && input is TrivialViewRequest6) {
                    sduiLog("RequestResult.Pending for $currentInput", tag = "ZoneGraph2 > request")
                    return refs
                }
                sduiLog("RequestResult.Failed or null for $currentInput", tag = "ZoneGraph2 > request")
                requests[currentInput.requestId] = RequestResult.Pending
                when {
                    update == StaticVoid6::class -> {
                        val computingInput = currentInput.toComputingViewRequest6()
                        val domain = StaticVoid6(computingInput) as D
                        val sduiDomain = getComputedSubdomain(computingInput.domainId) ?: ComputedSduiDomain(
                            domain = domain,
                            sdui = create(domain, computingInput.requestId),
                            domainId = computingInput.domainId
                        ).also {
                            sduiDomains[computingInput.domainId] = it
                        }
                        val updatedSduiDomain = sduiDomain.copy(
                            sdui = update(computingInput, sduiDomain.sdui, domain)
                        )
                        val result = Success(output = listOf(updatedSduiDomain), input = computingInput)
                        requests[currentInput.requestId] = result
                        send2(result.output, listOf(computingInput.domainId), computingInput)

                        val allRequests =
                            internalZoneMap.mapNotNull { it.value.requests[computingInput.requestId]?.let { v -> it.key + "|" + computingInput.requestId } }
                        sduiLog(allRequests, tag = "X > ZoneGraph2 > Static > request")

                        internalZoneMap.forEach {
                            it.value.requests[computingInput.requestId]?.let { requestResult ->
                                (requestResult as? Success<*>)?.let { success ->
                                    if (it.key != name) {
                                        send2(success.output, (success.input as? DomainViewRequest6)?.domainIds.orEmpty(), input)
                                    }
                                }
                            }
                        }
                    }

                    currentInput.domainIds.filterNot { sduiDomains[it] != null && sduiDomains[it] !is PendingSduiDomain }.isEmpty() -> {
                        val sduiDomains = currentInput.domainIds.mapNotNull { getComputedSubdomain(it) }
                        val updatedDomains = sduiDomains.map {
                            it.copy(
                                sdui = update(
                                    input = currentInput.toComputingViewRequest6(it.domainId),
                                    sduiInput2 = it.sdui,
                                    domain = it.domain,
                                )
                            )
                        }
                        val result = Success(output = updatedDomains, input = currentInput)
                        requests[currentInput.requestId] = result
                        send2(result.output, currentInput.domainIds, input)
//
//                        val allRequests =
//                            internalZoneMap.mapNotNull { it.value.requests[currentInput.requestId]?.let { v -> it.key + "|" + currentInput.requestId } }
//                        sduiLog(allRequests, tag = "X > ZoneGraph2 > request")

                        internalZoneMap.forEach {
                            it.value.requests[currentInput.requestId]?.let { requestResult ->
                                (requestResult as? Success<*>)?.let { success ->
                                    if (it.key != name) {
                                        send2(success.output, (success.input as? DomainViewRequest6)?.domainIds.orEmpty(), input)
                                    }
                                }
                            }
                        }
                    }

                    else -> {
                        val requestDomains = currentInput.domainIds.filterNot { sduiDomains.containsKey(it) }
                        val domainRequest = requestDomains.map { requestDomain(currentInput, it) }
                        domainRequest.forEach { publish(it) }
                    }
                }
            }
        }

        return refs
    }

    /**
     * Augment Request will finally send the sduis when the entire list is complete
     */
    fun augmentRequest(input: DomainViewRequest6, domain: DomainEvent6) {
        val sdui = create(domain as D, input.requestId)
        val sduiDomain = ComputedSduiDomain(domain, sdui, domain.domainId)
        sduiDomains[domain.domainId] = sduiDomain
        request(input)
    }

    fun failedDomainRequest(input: DomainViewRequest6, domainId: String) {
        sduiDomains[domainId] = FailedSduiDomain(domainId)
        request(input)
    }

    fun getComputedSubdomain(domainId: String) = sduiDomains[domainId] as? ComputedSduiDomain<D>

    val sduiDomains = mutableMapOf<String, SduiDomain<D>>()
    val requests = mutableMapOf<String, RequestResult>() // <---- When this is updated
    val subscribedRequests =
        mutableMapOf<String, List<String>>() // <---- This is checked to see if anyone is waiting on that request if computed from scratch

    sealed class SduiDomain<D : ScopedEvent> {
        abstract val domainId: String

        data class PendingSduiDomain<D : ScopedEvent>(
            override val domainId: String,
        ) : SduiDomain<D>()
        data class ComputedSduiDomain<D : ScopedEvent>(
            val domain: D,
            val sdui: SDUIInput2,
            override val domainId: String,
        ) : SduiDomain<D>()

        data class FailedSduiDomain<D : ScopedEvent>(
            override val domainId: String,
        ) : SduiDomain<D>()
    }


    sealed class RequestResult {
        data class Success<D : ScopedEvent>(
            val output: List<ComputedSduiDomain<D>>,
            val input: ViewRequest6,
            val refs: Map<String, List<String>> = mapOf() // TODO we somewhat need to compute views that were promised as they are not included in the request
        ) : RequestResult()

        data object Pending : RequestResult()
        data object Failed : RequestResult() // TODO to do at some point
    }

    /**
     * In theory this could be a perfect way to register a streaming interest in requests that update
     * Need sessionDependency send to indicate if sending was successful or not so we can remove from pendingRequests
     */

    companion object {
        inline fun <reified D : DomainEvent6> create(
            name: String,
            crossinline create: (update: D, requestId: String) -> SDUIInput2,
            crossinline update: ((input: DomainViewRequest6, sduiInput2: SDUIInput2, domain: D) -> SDUIInput2) = { _, sduiInput2, _ -> sduiInput2 },
            crossinline getPromise: (input: DomainViewRequest6) -> List<Pair<RefState2, DomainEvent6?>> = {
                listOf(
                    Pair(
                        RefState2(id = Id2.StateId2(name)),
                        null
                    )
                )
            },
            crossinline initiateInput: (input: UninitiatedViewRequest6) -> TrivialViewRequest6 = { input ->
                val id = input.id.name + input.id.scope
                TrivialViewRequest6(
                    id = input.id,
                    ctx = input.ctx,
                    listCtx = input.listCtx,
                    requesterId = input.requesterId,
                    requestId = id,
                    domainIds = listOf(id),
                )
            },
            crossinline requestDomain: (input: InitiatedViewRequest6, sduiDomainId: String) -> DomainEvent6 = { _, _ -> error("Not implemented requestDomain for $name") },
        ) = object : Zone<D>(D::class) {
            override val name = name
            override fun create(update: D, requestId: String) = create(update, requestId)
            override fun update(input: DomainViewRequest6, sduiInput2: SDUIInput2, domain: D) =
                update(input, sduiInput2, domain)

            override fun getPromise(input: DomainViewRequest6) = getPromise(input)
            override fun requestDomain(input: InitiatedViewRequest6, sduiDomainId: String): DomainEvent6 =
                requestDomain(input, sduiDomainId)

            override fun initiateInput(input: UninitiatedViewRequest6) = initiateInput(input)
        }

        lateinit var internalZoneMap: Map<String, Zone<out DomainEvent6>> // TODO make this be the source of truth and not direct state
    }
}