package best.nyan.lightswallow.server

import best.nyan.lightswallow.core.sandbox.loadSystemLibrary
import best.nyan.lightswallow.server.config.AppConfig
import io.quarkus.runtime.ShutdownEvent
import io.quarkus.runtime.StartupEvent
import javax.enterprise.context.ApplicationScoped
import javax.enterprise.event.Observes

/**
 * Lifecycle for LightSwallow Server Application
 */
@ApplicationScoped
class LightSwallowServerLifecycle(
    val appConfig: AppConfig
) {

    fun onStart(@Observes ex: StartupEvent) {
        // Load libraries
        loadSystemLibrary(appConfig.libPath)

        println("Server started")
    }

    fun onShutdown(@Observes ex: ShutdownEvent) {
        println("Server shutdown")
    }

}
