package best.nyan.lightswallow.server.config

import org.eclipse.microprofile.config.inject.ConfigProperty
import javax.enterprise.context.ApplicationScoped

/**
 * Basic config of the app
 */
@ApplicationScoped
class AppConfig(
    /**
     * Path of JNI Lib (the `.so` file)
     */
    @ConfigProperty(name = "app.path.lib")
    val libPath: String,

    /**
     * Path for chroot
     */
    @ConfigProperty(name = "app.path.chroot")
    val chrootPath: String,

    /**
     * Path for chdir
     */
    @ConfigProperty(name = "app.path.chdir")
    val chdirPath: String,

    /**
     * Path for home (the `chdir` inside)
     */
    @ConfigProperty(name = "app.path.home")
    val homePath: String
)