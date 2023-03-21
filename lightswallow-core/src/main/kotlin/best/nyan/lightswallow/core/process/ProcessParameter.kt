package best.nyan.lightswallow.core.process

import java.util.*

/**
 * The parameter for the environment
 */
data class ProcessParameter(
    /**
     * Name for the process task, determines the name of cgroups and namespaces inside.
     */
    var name: String = UUID.randomUUID().toString(),
    /**
     * The executable file to be run
     */
    var executable: String = "",
    /**
     * The parameters to be passed to the executable.
     *
     * Caution: The first parameter should be the same to executable value.
     */
    var parameters: List<String> = toSafetyJavaList(listOf()),
    /**
     * Hostname
     */
    var hostname: String = UUID.randomUUID().toString(),
    /**
     * Chroot path, mounted readonly for the processes as / .
     */
    var chrootPath: String = "/",
    /**
     * Working path for chdir
     */
    var chdirPath: String = "/home",
    /**
     * Max time limit (millisecond), -1 for no limit.
     */
    var timeLimit: Long = -1,
    /**
     * Max memory limit (byte), -1 for no limit.
     */
    var memoryLimit: Long = -1,
    /**
     * output limit
     */
    var outputLimit: Long = -1,
    /**
     * Max process and child process count, -1 for no limit.
     */
    var processLimit: Int = 10,
    /**
     * Mounts
     */
    var mounts: List<MountPair> = toSafetyJavaList(listOf()),
    /**
     * The user uid to run programs, -1 for no limit.
     */
    var userUid: Int = -1,
    /**
     * The user gid to run programs, -1 for no limit.
     */
    var userGid: Int = -1,
    /**
     * Stack size limited by `setrlimit`, -1 for no limit.
     */
    var stackSize: Long = -2,
    /**
     * The count of CPU cores to run programs, -1 for no limit.
     */
    var cpuCoreCnt: Int = -1,
    /**
     * Environment variables, such as PATH and so on.
     */
    var environments: List<String> = toSafetyJavaList(listOf()),
    /**
     * Whether mount /proc or not
     */
    var mountProc: Boolean = true,
    /**
     * Redirect stdio before chroot
     */
    var redirectIOBeforeChroot: Boolean = true,
    /**
     * stdio in
     */
    var stdin: String = "",
    /**
     * stdio out
     */
    var stdout: String = "",
    /**
     * stdio error
     */
    var stderr: String = "",
) {

    /**
     * Set the executable and parameters
     */
    fun setTarget(executable: String, parameters: List<String>): ProcessParameter {
        this.executable = executable
        this.parameters = toSafetyJavaList(parameters)
        return this
    }

    fun paramPathReplace(oldValue: String, newValue: String) {
        stdin = stdin.replace(oldValue, newValue)
        stdout = stdout.replace(oldValue, newValue)
        stderr = stderr.replace(oldValue, newValue)
    }

}

/**
 * Mount pair, determine a mount relation
 */
data class MountPair(
    val sourcePath: String,
    val targetPath: String,
    val readonly: Boolean = true
)

/**
 * avoid JNI crash when load kotlin list
 */
fun <T> toSafetyJavaList(list: List<T>): ArrayList<T> {
    val result = ArrayList<T>()
    list.forEach { result.add(it) }
    return result
}
