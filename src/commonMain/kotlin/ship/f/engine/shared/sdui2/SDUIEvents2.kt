package ship.f.engine.shared.sdui2

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.Resource
import ship.f.engine.shared.core.Event
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.Meta2
import ship.f.engine.shared.utils.serverdrivenui2.config.meta.models.PopulatedSideEffectMeta2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2.MetaId2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2.MetaId2.Companion.none
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2.StateId2
import ship.f.engine.shared.utils.serverdrivenui2.state.State2

// Events Accepted by SDUISubPub2
@Serializable
@SerialName("SDUIInput2")
data class SDUIInput2(
    val id: MetaId2 = none,
    val sideEffectId: MetaId2 = none,
    val states: List<State2> = listOf(),
    val orderedStates: List<StateId2> = listOf(),
    val metas: List<Meta2> = listOf(),
) : Event()

// Events Emitted by SDUISubPub2
@Serializable
@SerialName("SDUISideEffect2")
data class SDUISideEffect2(
    val sideEffect: PopulatedSideEffectMeta2,
) : Event()

@Serializable
@SerialName("SDUIViewRequest2")
data class SDUIViewRequest2(
    val stateId2: StateId2,
) : Event()

@Serializable
@SerialName("SDUIReactiveInput2")
data class SDUIReactiveInput2(
    val id: MetaId2 = none,
    val states: List<StateId2> = listOf(),
    val metas: List<Meta2> = listOf(),
) : Event()

// TODO cannot use serializable until we create one for Resource
// TODO Should probably leave the app to override this method using some kind of lambda
data class SDUIConfig2(
    val projectName: String,
    val resources: Map<String, Resource> = mapOf(),
    val vectors: Map<String, ImageVector> = mapOf(),
) : Event()
