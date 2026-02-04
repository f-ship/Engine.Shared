package ship.f.engine.shared.sdui2

import androidx.compose.ui.graphics.vector.ImageVector
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import org.jetbrains.compose.resources.Resource
import ship.f.engine.shared.core.Event
import ship.f.engine.shared.core.defaultScope2
import ship.f.engine.shared.utils.serverdrivenui2.config.action.models.Action2
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
    val part: Part = Part(1,1),
    val metas: List<Meta2> = listOf(),
    val actions: List<Action2> = listOf(),
) : Event() {
    @Serializable
    @SerialName("Part")
    data class Part(val current: Int, val total: Int) {
        companion object {
            const val ALL = -1
        }
    }
}

@Serializable
@SerialName("Complete")
data class Complete(
    val id: MetaId2 = none,
) : Event()

@Serializable
@SerialName("SDUIOutput2")
data class RerequestMissingParts(
    val id: MetaId2 = none,
    val parts: Set<Int>,
) : Event()

@Serializable
@SerialName("SDUIError2")
data class SDUIError2(
    val id: MetaId2 = none,
    val error: String,
) : Event()

@Serializable
@SerialName("MultiSDUIInput2")
data class MultiSDUIInput2(
    val values: List<SDUIInput2>,
    val viewRequest: SDUIViewRequest5,
    val parts: Set<Int>,
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
@SerialName("SDUIData")
data class SDUIData(
    val id: MetaId2,
    val data: ByteArray,
) : Event() {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as SDUIData

        if (!data.contentEquals(other.data)) return false

        return true
    }

    override fun hashCode(): Int {
        return data.contentHashCode()
    }
}

@Serializable
@SerialName("SDUIViewRequest2")
data class SDUIViewRequest5(
    val parentZoneStateId2: StateId2? = null,
    val zoneStateId2: StateId2,
    val zones: List<String> = listOf(defaultScope2), // TODO to rewrite engine to use a simpler version of scoping
    val childrenZoneStateIds2: List<StateId2> = listOf(), // TODO to allow sub querying of children zones
    val requesterId: String, // either userId or system
    val userId: String = "", //This needs to be filled
    val requestId: String, // Random string ties all returned events to together
    val context: Map<String, String> = mapOf(),
) : Event()

@Serializable
@SerialName("SDUIClientRequest4")
data class SDUIClientRequest4(
    val id: StateId2,
) : Event()

// TODO cannot use serializable until we create one for Resource
// TODO Should probably leave the app to override this method using some kind of lambda
data class SDUIConfig2(
    val projectName: String,
    val resources: Map<String, Resource> = mapOf(),
    val vectors: Map<String, ImageVector> = mapOf(),
) : Event()
