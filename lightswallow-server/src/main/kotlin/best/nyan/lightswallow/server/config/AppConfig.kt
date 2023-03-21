package best.nyan.lightswallow.server.config

import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.enterprise.context.ApplicationScoped

/**
 * Basic config of the app
 */
@ApplicationScoped
class AppConfig(
    /**
     * Server ID
     */
    @ConfigProperty(name = "app.server-id")
    val serverId: String,
    /**
     * Path of JNI Lib (the `.so` file)
     */
    @ConfigProperty(name = "app.path.lib")
    val libPath: String
)
