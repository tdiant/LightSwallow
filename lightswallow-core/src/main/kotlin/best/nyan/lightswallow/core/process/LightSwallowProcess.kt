package best.nyan.lightswallow.core.process

import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.core.result.ProcessResultType
import best.nyan.lightswallow.core.sandbox.SandboxHook
import java.util.*
import kotlin.math.max

/**
 * Process
 */
class LightSwallowProcess(
    /**
     * The uuid for the process.
     *
     * UUID should be unique and the default value is generated by random,
     * just for internal management in LightSwallow System.
     */
    val uuid: UUID = UUID.randomUUID(),
    /**
     * Core name
     */
    val name: String,
    /**
     * Sandbox Hook
     */
    val sandboxHook: SandboxHook = SandboxHook(),
    /**
     * Parameters
     */
    val parameter: ProcessParameter
) {

    /**
     * The instance created time for the process
     */
    val createdTime = System.currentTimeMillis()

    private var registerFlag = false

    val isStart get() = registerFlag

    /**
     * Start the process
     */
    fun start(): ProcessResult {
        if (registerFlag)
            throw RuntimeException("Cannot start twice for same process")
        registerFlag = true

        val arr = sandboxHook.startSandbox(parameter)
        val startTime = System.currentTimeMillis()

        //Press WaitForProcess
        val execResultArr = sandboxHook.waitForProcess(arr)
            .split(";")
            .map { it.toInt() }

        registerFlag = false

        //Memory usage
        val memoryUsage = sandboxHook.readMemoryUsage(name)

        //Time usage
        val usageSubTime = System.currentTimeMillis() - startTime  //Time counted by TimeLimitExceededCheckThread
        val usageCgroupTime = sandboxHook.readTimeUsage(name)  //Time counted by Linux cgroup cpuacct
        val timeUsage = max(usageCgroupTime, usageSubTime)

        val execResultCode = execResultArr[0]
        val execResultStatus = execResultArr[1]

        if (execResultStatus == 0) {

            if (timeUsage >= parameter.timeLimit) {
                ProcessResult(
                    ProcessResultType.TIME_LIMIT_EXCEEDED,
                    parameter.timeLimit + 1,
                    memoryUsage,
                    execResultCode
                )
            }

            if (memoryUsage >= parameter.memoryLimit) {
                ProcessResult(
                    ProcessResultType.MEMORY_LIMIT_EXCEEDED,
                    timeUsage,
                    parameter.memoryLimit + 1,
                    execResultCode
                )
            }

            //todo

            return ProcessResult(ProcessResultType.SUCCESS_EXIT, timeUsage, memoryUsage, execResultCode)
        } else {
            return ProcessResult(ProcessResultType.RUNTIME_ERROR, timeUsage, memoryUsage, execResultCode)
        }
    }

    /**
     * Destroy the process.
     */
    fun destroy() {
        if (!registerFlag)
            return
//            throw RuntimeException("Cannot destroy process environment before starting the process")
        sandboxHook.destroyEnvironment(name, false)
        registerFlag = false
    }
}

