package ai.plusonelabs.agent

data class ModelResponse<M>(
    val output: M,
    val usage: Any?,
    val responseId: String,
)

data class SingleStepResult<M>(
    val modelResponse: ModelResponse<M>,
    val nextStep: NextStep<M>,
)

sealed class NextStep<M> {
    data class FinalOutput<T>(val output: T) : NextStep<T>()
    class RunAgain<T> : NextStep<T>()
}
