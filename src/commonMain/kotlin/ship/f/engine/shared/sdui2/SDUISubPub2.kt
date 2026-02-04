package ship.f.engine.shared.sdui2

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Resource
import ship.f.engine.shared.core.*
import ship.f.engine.shared.sdui2.SDUISubPub2.SDUIState2
import ship.f.engine.shared.utils.serverdrivenui2.client3.Client3.Companion.client3
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.NavigationConfig2
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.PopulatedSideEffectMeta2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2.StateId2
import ship.f.engine.shared.utils.serverdrivenui2.ext.getRandomString
import ship.f.engine.shared.utils.serverdrivenui2.ext.sduiLog
import ship.f.engine.shared.utils.serverdrivenui2.state.UnknownState2

class SDUISubPub2 : SubPub<SDUIState2>(
    requiredEvents = setOf(SDUIConfig2::class),
    nonRequiredEvents = setOf(SDUIInput2::class, ToastEvent::class),
) {
    data class SDUIState2(
        val projectName: String? = null,
        val resources: Map<String, Resource> = mapOf(),
        val vectors: Map<String, ImageVector> = mapOf(),
        val toast: ToastEvent? = null,
    ) : State()

    override fun initState() = SDUIState2()
    override fun postInit() {
        val emitViewRequestHandler: (StateId2) -> Unit = {
            coroutineScope.launch {
                publish(SDUIClientRequest4(id = it))
            }
        }
        val emitSideEffectHandler: (PopulatedSideEffectMeta2) -> Unit = { populatedSideEffect ->
            coroutineScope.launch { // TODO check to see if this is really necessary
                publish(SDUISideEffect2(populatedSideEffect)) {
                    onceAny(
                        ExpectationBuilder(
                            expectedEvent = SDUIInput2::class,
                            onCheck = { populatedSideEffect.onExpected.contains(sideEffectId) },
                            on = {
                                try {
                                    // TODO completely hacked, not a general solution
                                    populatedSideEffect.onExpected[sideEffectId]?.forEach { exp ->
                                        coroutineScope.launch {
                                            delay(2000L)
                                            exp.second.run3(
                                                state = client3.get(exp.first),
                                                client = client3
                                            )
                                        }
                                    }
                                } catch (e: Exception) {
                                    coroutineScope.launch {
                                        publish(
                                            ToastEvent(
                                                message = "Sorry this expect cannot be performed at this time. Please try again later.",
                                                durationMs = 2000L,
                                                actionText = "Dismiss",
                                                toastType = ToastEvent.ToastType.Warning,
                                                key = getRandomString(),
                                            )
                                        )
                                    }
                                }
                            }
                        )
                    )
                }
            }
        }
        val localSideEffectHandler: (PopulatedSideEffectMeta2) -> Unit = {

        }
        client3.emitViewRequest = emitViewRequestHandler
        client3.emitSideEffect = emitSideEffectHandler
        client3.emitLocalEffect = localSideEffectHandler
    }

    override suspend fun ScopedEvent.onEvent() {
        state.value = getOrRun<SDUIConfig2>().firstOrNull()?.let {
            sduiLog("Received SDUIConfig2 event ${it.projectName} in SDUISubPub2", tag = "EngineX")
            state.value.copy(
                projectName = it.projectName,
                resources = it.resources,
                vectors = it.vectors,
            )
        } ?: state.value

        le<SDUIInput2> {
            sduiLog("Received SDUIInput2 event ${it.id}", tag = "SDUISubPub > SDUIInput2")
            try {
                client3.run {
                    it.states.forEach { state -> initState(state = state, forceUpdate = it.forceUpdate) }
                    it.metas.forEach { meta -> update(meta) }
                    it.metas.filterIsInstance<NavigationConfig2>().forEach { nav -> navigationEngine.navigate(nav.operation) }
                    it.actions.forEach { action ->
                        sduiLog("Running action ${action}", tag = "SDUISubPub > SDUIInput2 > actions")
                        action.run3(state = UnknownState2(), client = client3)
                    }
                }
                client3.commit()
            } catch (e: Exception) {
                sduiLog("SDUIInput2 error > $e", e, tag = "NavigationEngine > SDUIInput2")
                publish(
                    ToastEvent(
                        message = "Sorry this input cannot be performed at this time. Please try again later.",
                        durationMs = 2000L,
                        actionText = "Dismiss",
                        toastType = ToastEvent.ToastType.Warning,
                    )
                )
            }
        }

        le<ToastEvent> {
            sduiLog("Received ToastEvent event ${it.message}", tag = "EngineX")
            state.value = state.value.copy(toast = it)
        }

        le<SDUIError2> {
            state.value = state.value.copy(
                toast = ToastEvent(
                    message = it.error,
                    toastType = ToastEvent.ToastType.Error,
                    durationMs = 2000L,
                    key = it.id.scope,
                )
            )
        }
    }
}