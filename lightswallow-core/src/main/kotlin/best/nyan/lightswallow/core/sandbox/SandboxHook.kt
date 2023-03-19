package best.nyan.lightswallow.core.sandbox

import best.nyan.lightswallow.core.process.ProcessParameter

/**
 * Hook for the sandbox, via JNI.
 */
class SandboxHook {

    /**
     * Start the sandbox
     */
    @JvmName("startSandbox")
    external fun startSandbox(parameters: ProcessParameter): IntArray

    /**
     * Wait for the process
     */
    @JvmName("waitForProcess")
    external fun waitForProcess(buf: IntArray): String

    /**
     * Kill the running task
     */
    @JvmName("destroyEnvironment")
    external fun destroyEnvironment(cgroupName: String, removeCgroup: Boolean)

    @JvmName("getCgroupPropertyAsLong")
    external fun getCgroupPropertyAsLong(controller: String, cgroupName: String, property: String): Long

    @JvmName("readTimeUsage")
    external fun readTimeUsage(cgroupName: String): Long

    @JvmName("readMemoryUsage")
    external fun readMemoryUsage(cgroupName: String): Long

    /**
     * PID for the running process container
     */
    var containerPid: Int = -1
        private set

    /**
     * This method is called by system itself,
     * when the child process PID has been gotten by the sandbox.
     */
    fun callbackContainerPid(pid: Int) {
        containerPid = pid
    }

}
