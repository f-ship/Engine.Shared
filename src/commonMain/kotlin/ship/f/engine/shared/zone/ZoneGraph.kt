package ship.f.engine.shared.zone

import ship.f.engine.shared.core.ScopedEvent
import ship.f.engine.shared.sdui2.SDUIInput2
import kotlin.reflect.KClass

sealed class ZoneGraph {
    abstract val name: String
    abstract val cacheable: CacheMode

    abstract val update: KClass<out ScopedEvent>
    abstract val get: KClass<out ScopedEvent> // TODO going to replace with a cheap alternative till we have scopes working

    sealed class Parent : ZoneGraph() {
        abstract val children: MutableMap<String, Join>

        data class Root(
            override val name: String,
            override val cacheable: CacheMode = CacheMode.None,
            override val update: KClass<out ScopedEvent>,
            override val get: KClass<out ScopedEvent>,
            override val children: MutableMap<String, Join> = mutableMapOf(),
        ) : Parent()

        data class Node(
            override val name: String,
            override val cacheable: CacheMode = CacheMode.None,
            override val update: KClass<out ScopedEvent>,
            override val get: KClass<out ScopedEvent>,
            override val children: MutableMap<String, Join> = mutableMapOf(),
            val parent: String,
        ) : Parent()
    }


    data class Leaf(
        override val name: String,
        override val cacheable: CacheMode = CacheMode.None,
        override val update: KClass<out ScopedEvent>,
        override val get: KClass<out ScopedEvent>,
        val parent: String,
    ) : ZoneGraph()

    data class Join(
        val zone: ZoneGraph,
        val mode: Mode = Mode.Include,
    )

    /**
     * Hot is inferred by the requester ID, if Null, then it's not Hot
     * Deep + Hot will be the most common use case > Stream in as much data to a client ensuring children are returned all or nothing.
     * Lazy + Hot is useful if it doesn't matter when the children are returned and when they no more children are expected
     * Shallow + Hot is useful if you don't even care about the children being returned automatically, usually when the user needs to do something to trigger this
     * Deep + Cold can be useful for the system for deep cache invalidation
     * Shallow + Cold can be useful for the system for shallow cache invalidation?
     */

    sealed class Mode {
        data object Include : Mode() // If all children should be computed, given the limit
        data object Lazy :
            Mode() // Instead of the children being batched into a list, the children will be sent ad hoc as they are computed

        data object Ignore : Mode() // Don't compute at all
    }

    sealed class CacheMode {
        data object Static : CacheMode() // TODO need to use this to skip running dynamic mapper

        data object Dynamic : CacheMode()
        data object None : CacheMode()
    }

    sealed class ViewResponse {
        data class BuildingViewResponse(
            val sduiInputs: List<SDUIInput2> = listOf(),
            val originName: String,
        ) : ViewResponse()

        data class PendingViewResponse(
            val sduiInputs: List<SDUIInput2> = listOf(),
            val originName: String,
        ) : ViewResponse()

        data object ReceivedViewResponse : ViewResponse()
    }


    companion object Companion {

        fun toFlatZoneGraph(join: Join, map: MutableMap<String, Join>): Map<String, Join> = map.apply {
            this[join.zone.name] = join
            if (join.zone is Parent) join.zone.children.values.forEach { child -> toFlatZoneGraph(child, this) }
        }

        fun toDescendants(join: Join, mutableList: MutableList<String> = mutableListOf()){
            join.zone.name.also { mutableList.add(it) }
            if (join.zone is Parent) join.zone.children.values.forEach { child -> toDescendants(child, mutableList) }
        }
    }
}