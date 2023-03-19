package best.nyan.lightswallow.server

import best.nyan.lightswallow.core.sandbox.loadSystemLibrary
import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.util.FileUtils.checkDirectoryExists
import io.quarkus.runtime.StartupEvent
import java.io.IOException
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

        // Check necessary directories
        if(!checkDirectoryExists(appConfig.chrootPath) || !checkDirectoryExists(appConfig.chdirPath))
            throw IOException("Could not found the chroot | chdir path, please ensure they are exists.")
    }

}