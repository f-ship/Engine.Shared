package ship.f.engine.shared.sdui2

import io.ktor.client.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.plugins.websocket.*
import io.ktor.serialization.kotlinx.json.*
import ship.f.engine.shared.core.Dependency
import ship.f.engine.shared.utils.serverdrivenui2.json.json2
import kotlin.time.Duration.Companion.seconds

class KtorClientDependency2 : Dependency() {
    val client = HttpClient {
        install(ContentNegotiation) {
            json(
                json = json2
            )
        }
        install(WebSockets) {
            pingInterval = 15.seconds
        }
    }
}