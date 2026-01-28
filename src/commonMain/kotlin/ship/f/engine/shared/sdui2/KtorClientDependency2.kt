package ship.f.engine.shared.sdui2

import io.ktor.client.*
import io.ktor.client.plugins.websocket.*
import ship.f.engine.shared.core.Dependency
import kotlin.time.Duration.Companion.seconds

class KtorClientDependency2 : Dependency() {
    val client = HttpClient {
        install(WebSockets) {
            pingInterval = 15.seconds
        }
    }
}