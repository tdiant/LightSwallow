package best.nyan.lightswallow.server.service

import best.nyan.lightswallow.core.LightSwallowEntity
import best.nyan.lightswallow.core.LightSwallowEntityTask
import best.nyan.lightswallow.core.SandboxStatus
import best.nyan.lightswallow.core.process.ProcessParameter
import best.nyan.lightswallow.core.result.ProcessResult
import best.nyan.lightswallow.core.result.ProcessResultType
import best.nyan.lightswallow.server.config.AppConfig
import best.nyan.lightswallow.server.entity.RunningSandboxRuntimeRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequest
import best.nyan.lightswallow.server.entity.SandboxRunnableRequestTask
import best.nyan.lightswallow.server.util.FileUtils
import io.vertx.core.Promise
import java.io.File
import java.io.IOException
import java.util.concurrent.Executors
import javax.enterprise.context.ApplicationScoped
import kotlin.io.path.Path
import kotlin.io.path.pathString

@ApplicationScoped
class SandboxRunnableRequestService(
    private val appConfig: AppConfig
) {

    private val threadPool = Executors.newFixedThreadPool(appConfig.threadPoolSize)

    /**
     * Running requests
     */
    val runningRequests = mutableListOf<RunningSandboxRuntimeRequest>()

    fun runRequest(request: SandboxRunnableRequest): RunningSandboxRuntimeRequest? {
        //Check if queue fulled
        if (runningRequests.count { it.status == RunningSandboxRuntimeRequest.RunningStatus.RUNNING } > appConfig.threadPoolSize ||
            runningRequests.count { it.status == RunningSandboxRuntimeRequest.RunningStatus.READY } > appConfig.threadPoolSize * 2)
            return null

        val futureResultPromise = Promise.promise<SandboxRunnableRequestResult>()
        val runningRequest = RunningSandboxRuntimeRequest(request, futureResultPromise.future())

        threadPool.execute {
            cleanRunningRequests()
            runningRequest.pressRunning()
            val finalResult = runAndWaitRequest(request, appConfig.serverId)
            futureResultPromise.complete(finalResult)
        }

        runningRequests.add(runningRequest)

        return runningRequest
    }

    private fun runAndWaitRequest(request: SandboxRunnableRequest, serverId: String): SandboxRunnableRequestResult {
        val startTime = System.currentTimeMillis()

        val chrootPath = request.chrootPath
        val chdirRootPath = request.chdirRootPath
        val chdirPath = Path(chdirRootPath, request.id).pathString

        // Create sandbox entity
        val sandbox = LightSwallowEntity(
            chrootPath = chrootPath,
            chdirPath = chdirPath,
            homePath = request.homePath
        )
        sandbox.loadSandbox()
        if (sandbox.status != SandboxStatus.READY) {
            return SandboxRunnableRequestResult(
                id = request.id,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                serverId = serverId,
                result = mapOf()
            )
        }

        // Run tasks
        val taskFutures = buildList {
            request.tasks.forEach { task ->
                val futureTaskResultPromise = Promise.promise<SandboxRunnableRequestTaskResult>()

                threadPool.execute {
                    cleanRunningRequests()
                    val result = runAndWaitRequestSingleTask(request, task, serverId, sandbox, chrootPath, chdirPath)
                    futureTaskResultPromise.complete(result)
                }

                add(futureTaskResultPromise.future())
            }
        }

        // Waiting tasks
        while (taskFutures.count { !it.isComplete } != 0) {
            Thread.sleep(0)
        }

        // Collect results
        val resultMap = buildMap {
            taskFutures.forEach {
                val result = it.result()
                put(result.id, result)
            }
        }

        val endTime = System.currentTimeMillis()

        return SandboxRunnableRequestResult(
            id = request.id,
            startTime = startTime,
            endTime = endTime,
            serverId = serverId,
            result = resultMap
        )

    }


    //
    //        Thread.sleep(0)
    //        System.gc()
    //
    //        if (deleteDirectoryWhenFinished) {
    //            chdirFile.listFiles()?.forEach { it.delete() }
    //            chdirFile.delete()
    //        }
    //

    private fun runAndWaitRequestSingleTask(
        req: SandboxRunnableRequest,
        task: SandboxRunnableRequestTask,
        serverId: String,
        sandboxEntity: LightSwallowEntity,
        chrootPath: String,
        chdirPath: String
    ): SandboxRunnableRequestTaskResult {
        val startTime = System.currentTimeMillis()

        if (sandboxEntity.status != SandboxStatus.READY) {
            return SandboxRunnableRequestTaskResult(
                id = task.id,
                success = false,
                startTime = startTime,
                endTime = System.currentTimeMillis(),
                serverId = serverId,
                result = ProcessResult(ProcessResultType.RUNTIME_ERROR, -1L, -1L, -100),
                out = "",
                err = "Status not ready."
            )
        }

        // Check directory exists
        if (!FileUtils.checkDirectoryExists(chrootPath))
            throw IOException("Target path not exists")

        val chdirFile = File(chdirPath)
        chdirFile.mkdirs()

        // Prepared files
        req.prepareFiles.forEach { (filename, content) ->
            val filePath = Path(chdirPath, filename).pathString
            FileUtils.writeStringToFile(content, filePath)
        }

        // Replace the paths
        req.pathReplacement.forEach { (x, y) ->
            task.sandboxParameter.paramPathReplace(x, y)
        }
        task.sandboxParameter.paramPathReplace("{root}", chdirPath)

        fun ensureIOFile(param: ProcessParameter, filePath: String): Boolean {
            return if (param.redirectIOBeforeChroot) {
                FileUtils.ensureFileExists(filePath)
            } else {
                if (filePath.isNotEmpty()) {
                    FileUtils.ensureFileExists(Path(chdirPath, filePath).pathString)
                } else false
            }
        }

        fun ensureIOAbsolutePath(param: ProcessParameter, filePath: String): String? {
            if (!ensureIOFile(param, filePath))
                return null
            return if (param.redirectIOBeforeChroot) {
                filePath
            } else {
                Path(chdirPath, filePath).pathString
            }
        }

        // Make sure IO files exists
        if (!ensureIOFile(task.sandboxParameter, task.sandboxParameter.stdin))
            task.sandboxParameter.stdin = ""
        if (!ensureIOFile(task.sandboxParameter, task.sandboxParameter.stdout))
            task.sandboxParameter.stdout = ""
        if (!ensureIOFile(task.sandboxParameter, task.sandboxParameter.stderr))
            task.sandboxParameter.stderr = ""

        // Run and collect results
        val result = sandboxEntity.runTask(
            task = LightSwallowEntityTask(
                sandboxParameter = task.sandboxParameter
            )
        )

        // IO outputs
        val ioOut = ensureIOAbsolutePath(task.sandboxParameter, task.sandboxParameter.stdout)?.let {
            if (req.readFromIOFiles)
                FileUtils.readFileToString(it)
            else ""
        } ?: ""
        val ioErr = ensureIOAbsolutePath(task.sandboxParameter, task.sandboxParameter.stderr)?.let {
            if (req.readFromIOFiles)
                FileUtils.readFileToString(it)
            else ""
        } ?: ""

        val endTime = System.currentTimeMillis()

        // Run and build result
        return SandboxRunnableRequestTaskResult(
            id = task.id,
            success = true,
            startTime = startTime,
            endTime = endTime,
            serverId = serverId,
            result = result,
            out = ioOut,
            err = ioErr
        )
    }

    private fun cleanRunningRequests() {
        runningRequests.removeIf {
            System.currentTimeMillis() - it.createTime > 300000 && //5min
                    it.status == RunningSandboxRuntimeRequest.RunningStatus.DEAD
        }
    }

}

data class SandboxRunnableRequestTaskResult(
    val id: String,
    val success: Boolean,
    val startTime: Long,
    val endTime: Long,
    val serverId: String,
    val result: ProcessResult,
    val out: String,
    val err: String
)

data class SandboxRunnableRequestResult(
    val id: String,
    val startTime: Long,
    val endTime: Long,
    val serverId: String,
    val result: Map<String, SandboxRunnableRequestTaskResult>
)

