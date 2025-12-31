package ship.f.engine.shared.sdui2

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import ship.f.engine.client.utils.networking.NetworkConnectivityObserver
import ship.f.engine.shared.core.Dependency

class ConnectivityDependency(
) : Dependency() {
    lateinit var observer: NetworkConnectivityObserver

    override fun init() {
//        observer = NetworkConnectivityObserver().apply {
//
//        }
    }

    val dependencyScope = CoroutineScope(Dispatchers.Default)

    // TODO Need to create dependencies per platform
    override fun postInit() {
//        observer.observe()
//            .onEach { status ->
//                val isConnected = status == ConnectivityObserver.Status.Available
//                publish(
//                    event = NetworkConnectivityEvent(isConnected = isConnected),
//                    reason = "Network Status Changed to $status"
//                )
//            }
//            .launchIn(dependencyScope)
    }
}
