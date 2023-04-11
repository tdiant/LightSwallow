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
    val libPath: String,

    /**
     * Size of request handling thread pool
     */
    @ConfigProperty(name = "app.thread-pool-size")
    val threadPoolSize: Int,

    /**
     * API Secret Key
     */
    @ConfigProperty(name = "app.secret-key", defaultValue = "")
    val secretKey: String

) {

    fun verifySecretKey(secretKey: String) =
        this.secretKey.isEmpty() || this.secretKey == secretKey

}
