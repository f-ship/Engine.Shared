package ship.f.engine.shared.zone

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import ship.f.engine.shared.core.ScopedEvent
import ship.f.engine.shared.core.defaultScope2
import ship.f.engine.shared.utils.serverdrivenui2.config.state.models.Id2

@Serializable
sealed class RequestStatus : ScopedEvent() {
    abstract val id: Id2
    abstract val retries: Int
    abstract val requestEvent: ScopedEvent
    override fun getScopes2(): List<String> = listOf(id.name+id.scope)

    @Serializable
    data class Requesting(
        override val id: Id2,
        override val retries: Int,
        override val requestEvent: ScopedEvent,
        val parts: Set<Int> = setOf(),
        val total: Int = 1,
    ) : RequestStatus()

    @Serializable
    data class Processing(
        override val id: Id2,
        override val retries: Int,
        override val requestEvent: ScopedEvent,
        val timeout: Long,
    ) : RequestStatus()

    @Serializable
    data class Transmitting(
        override val id: Id2,
        override val retries: Int,
        override val requestEvent: ScopedEvent,
        val timeout: Long,
        val parts: Set<Int>,
        val total: Int,
    ) : RequestStatus() {

        @Serializable
        @SerialName("Part")
        data class Part(
            val id: Id2,
            val index: Int,
            val scopedEvent: ScopedEvent
        ) : ScopedEvent() {
            override fun getScopes2(): List<String> = listOf(defaultScope2)
        }
    }

    @Serializable
    data class Complete(
        override val id: Id2,
        override val retries: Int,
        override val requestEvent: ScopedEvent,
    ) : RequestStatus()

    @Serializable
    data class Error(
        override val id: Id2,
        override val retries: Int,
        override val requestEvent: ScopedEvent,
        val error: Reason,
    ) : RequestStatus() {
        @Serializable
        sealed class Reason {
            abstract val message: String
            @Serializable
            data class Response(override val message: String) : Reason()
            @Serializable
            data class PartialResponse(override val message: String) : Reason()
        }
    }
}