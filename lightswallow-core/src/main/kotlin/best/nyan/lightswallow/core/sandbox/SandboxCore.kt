package best.nyan.lightswallow.core.sandbox

import best.nyan.lightswallow.core.process.LightSwallowProcess
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.core.result.ProcessResultType
import java.io.File

const val cgroupMemoryController = "memory"
const val cgroupCpuAcctController = "cpuacct"
const val cgroupPidsController = "pids"

class SandboxCore(
    val name: String
) {

    val sandboxHook = SandboxHook()

    fun runProcess(parameter: ProcessParameter): ProcessResult {
        val process = LightSwallowProcess(
            name = name,
            sandboxHook = sandboxHook,
            parameter = parameter.apply { this.name = this@SandboxCore.name }
        )

        var timeLimitKillFlag = false
        var outputLimitKillFlag = false

        // Time Limit Exceeded Check Thread
        object : Thread() {
            val startTime = System.currentTimeMillis()
            override fun run() {
                while (System.currentTimeMillis() - startTime < parameter.timeLimit * 1.2) {
                    sleep(0)
                }
                if (!process.isStart)
                    return
                process.destroy()
                timeLimitKillFlag = true
            }
        }.start()

        // Output Check Thread
        if (parameter.outputLimit >= 0) {
            object : Thread() {
                val startTime = System.currentTimeMillis()
                override fun run() {
                    while (System.currentTimeMillis() - startTime < parameter.timeLimit * 1.1) {
                        sleep(50)
                        if (!outputLimitKillFlag && File(parameter.stdout).length() > parameter.outputLimit) {
                            process.destroy()
                            outputLimitKillFlag = true
                        }
                        if (!outputLimitKillFlag && File(parameter.stderr).length() > parameter.outputLimit) {
                            process.destroy()
                            outputLimitKillFlag = true
                        }
                        if (outputLimitKillFlag) {
                            return
                        }
                    }
                }
            }.start()
        }

        val result = process.start()
        process.destroy()

        if (timeLimitKillFlag)
            return ProcessResult(
                ProcessResultType.TIME_LIMIT_EXCEEDED,
                parameter.timeLimit + 1,
                result.memory,
                result.returnCode
            )

        if (outputLimitKillFlag)
            return ProcessResult(
                ProcessResultType.OUTPUT_LIMIT_EXCEEDED,
                parameter.timeLimit + 1,
                parameter.memoryLimit + 1,
                result.returnCode
            )

        return result
    }

    fun destoryEnvironment() {
        sandboxHook.destroyEnvironment(name, true)
    }

    init {
        if (!loadSystemLibrariesFlag)
            throw Error("Please load libraries before using the core")
    }

}

private var loadSystemLibrariesFlag = false
fun loadSystemLibrary(libPath: String) {
    if (loadSystemLibrariesFlag)
        throw Error("Could not load libraries twice")
    System.load(libPath)
    loadSystemLibrariesFlag = true
}
