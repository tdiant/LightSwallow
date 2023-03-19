package best.nyan.lightswallow.core.result

/**
 * The process result
 */
data class ProcessResult(
    val status: ProcessResultType,
    val time: Long,
    val memory: Long,
    val returnCode: Int
)

enum class ProcessResultType(
    val code: Int
) {
    UNKNOWN(0),
    SUCCESS_EXIT(1),
    TIME_LIMIT_EXCEEDED(2),
    MEMORY_LIMIT_EXCEEDED(3),
    RUNTIME_ERROR(4),
    OUTPUT_LIMIT_EXCEEDED(5),
    CANCELLED(6),
    TRANSFER_ERROR(7),
    COMPILE_ERROR(8)
}
