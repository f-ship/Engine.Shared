package ship.f.engine.shared.sdui2

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.Resource
import ship.f.engine.shared.core.Event
import ship.f.engine.shared.core.defaultScope2
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
    val forceUpdate: Boolean = false,
    val states: List<State2> = listOf(),
    val metas: List<Meta2> = listOf(),
) : Event()

@Serializable
@SerialName("MultiSDUIInput2")
data class MultiSDUIInput2(
    val values: List<SDUIInput2>,
    val viewRequest: SDUIViewRequest2,
) : Event()

@Serializable
@SerialName("BroadcastSDUIInput2")
data class BroadcastSDUIInput2(
    val usersInput: SDUIInput2,
    val users: List<String>,
    val userInput: SDUIInput2,
    val user: String,
) : Event()

@Serializable
@SerialName("SendSDUIInput2")
data class SendSDUIInput2(
    val input: SDUIInput2,
    val user: String,
) : Event()

@Serializable
@SerialName("SendSDUIInput2")
data class AnonSendSDUIInput2(
    val input: SDUIInput2,
    val device: String,
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
    val parentZoneStateId2: StateId2? = null,
    val zoneStateId2: StateId2,
    val zones: List<String> = listOf(defaultScope2), // TODO to rewrite engine to use a simpler version of scoping
    val childrenZoneStateIds2: List<StateId2> = listOf(), // TODO to allow sub querying of children zones
    val requesterId: String, // either userId or system
    val userId: String = "", //This needs to be filled
    val requestId: String, // Random string ties all returned events to together
    val context: Map<String, String> = mapOf(),
) : Event()

// TODO cannot use serializable until we create one for Resource
// TODO Should probably leave the app to override this method using some kind of lambda
data class SDUIConfig2(
    val projectName: String,
    val resources: Map<String, Resource> = mapOf(),
    val vectors: Map<String, ImageVector> = mapOf(),
) : Event()
