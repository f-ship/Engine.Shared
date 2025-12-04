package ship.f.engine.shared.sdui2

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.Resource
import ship.f.engine.shared.core.ExpectationBuilder
import ship.f.engine.shared.core.State
import ship.f.engine.shared.core.SubPub
import ship.f.engine.shared.sdui2.SDUISubPub2.SDUIState2
import ship.f.engine.shared.utils.serverdrivenui2.client3.Client3.Companion.client3
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.NavigationConfig2
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.PopulatedSideEffectMeta2
import ship.f.engine.shared.utils.serverdrivenui2.ext.sduiLog

class SDUISubPub2 : SubPub<SDUIState2>(
    requiredEvents = setOf(SDUIConfig2::class),
    nonRequiredEvents = setOf(SDUIInput2::class),
) {
    data class SDUIState2(
        val projectName: String? = null,
        val resources: Map<String, Resource> = mapOf(),
        val vectors: Map<String, ImageVector> = mapOf(),
    ) : State()

    override fun initState() = SDUIState2()
    override fun postInit() {
        val client = getDependency(CommonClientDependency2::class).client
        val handler: (PopulatedSideEffectMeta2) -> Unit = { populatedSideEffect ->
            coroutineScope.launch { // TODO check to see if this is really necessary
                publish(SDUISideEffect2(populatedSideEffect)) {
                    onceAny(
                        ExpectationBuilder(
                            expectedEvent = SDUIInput2::class,
                            onCheck = { populatedSideEffect.onExpected.contains(sideEffectId) },
                            on = {
                                populatedSideEffect.onExpected[sideEffectId]?.forEach {
                                    it.second.run(
                                        state = client.get(it.first),
                                        client = client,
                                    )
                                }
                            }
                        )
                    )
                }
            }
        }
        client.emitSideEffect = handler
        client3.emitSideEffect = handler
    }

    override suspend fun onEvent() {
//        ge<SDUIConfig2> {
//            state.value = state.value.copy(
//                projectName = it.projectName,
//                resources = it.resources,
//                vectors = it.vectors,
//            )
//        }

        state.value = gev2<SDUIConfig2>().firstOrNull()?.let {
            sduiLog("Received SDUIConfig2 event ${it.projectName} in SDUISubPub2", tag = "EngineX")
            state.value.copy(
                projectName = it.projectName,
                resources = it.resources,
                vectors = it.vectors,
            )
        } ?: state.value

        le<SDUIInput2> {
            sduiLog("Received SDUIInput2 event ${it.id}", tag = "NavigationEngine > SDUIInput2")
//            getDependency(CommonClientDependency2::class).client.run {
//                it.states.forEach { state -> update(state) }
//                it.metas.forEach { meta -> update(meta) }
//                it.metas.filterIsInstance<NavigationConfig2>().forEach { nav -> navigate(nav) }
//            }
            client3.run {
                it.states.forEach { state -> initState(state) }
                it.metas.forEach { meta -> update(meta) }
                it.metas.filterIsInstance<NavigationConfig2>().forEach { nav -> navigationEngine.navigate(nav.operation) }
            }
            client3.commit()
        }
    }
}