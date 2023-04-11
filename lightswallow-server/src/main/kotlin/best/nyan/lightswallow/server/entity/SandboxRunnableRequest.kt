package best.nyan.lightswallow.server.entity

import best.nyan.lightswallow.core.process.ProcessParameter
import java.util.*

/**
 * The batch tasks for LightSwallow sandbox to run
 */
class SandboxRunnableRequest(
    /**
     * ID
     */
    val id: String = UUID.randomUUID().toString() + "_${System.currentTimeMillis()}",
    /**
     * Tasks need to run
     */
    val tasks: List<SandboxRunnableRequestTask> = listOf(),
    /**
     * Path replacement rules
     */
    val pathReplacement: Map<String, String> = mapOf(),
    /**
     * Path of chroot
     */
    val chrootPath: String = "/",
    /**
     * Path of chdir root
     */
    val chdirRootPath: String = "/home",
    /**
     * Path of home (working directory inside the sandbox)
     */
    val homePath: String = "/home",

    /**
     * Delete chdir directory when all processes done
     */
    val deleteDirectoryWhenFinished: Boolean = true,

    /**
     * Read content from IO files after all processes finished up
     */
    val readFromIOFiles: Boolean = false,

    /**
     * Prepare files, written to sandbox before launching processes
     */
    val prepareFiles: Map<String, String> = mapOf()
)

val defaultProcessParameter = ProcessParameter() //useless, just for adding a non-params constructor

data class SandboxRunnableRequestTask(
    val sandboxParameter: ProcessParameter = defaultProcessParameter
) {
    val id: String = UUID.randomUUID().toString()
}
