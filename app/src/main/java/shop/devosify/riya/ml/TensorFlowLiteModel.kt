@Singleton
class TensorFlowLiteModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val analyticsService: AnalyticsService
) {
    private var interpreter: Interpreter? = null
    private val vocabMap = mutableMapOf<String, Int>()
    private val embeddingSize = 128

    init {
        loadModel()
        loadVocabulary()
    }

    private fun loadModel() {
        try {
            val modelFile = loadModelFile()
            val options = Interpreter.Options().apply {
                setNumThreads(4)
                setUseNNAPI(true) // Use Neural Network API if available
            }
            interpreter = Interpreter(modelFile, options)
            analyticsService.logEvent("ml_model_loaded")
        } catch (e: Exception) {
            analyticsService.logError("ml_model_load_failed", e.message ?: "Unknown error")
        }
    }

    private fun loadModelFile(): MappedByteBuffer {
        return context.assets.openFd("embedded_model.tflite").let {
            FileInputStream(it.fileDescriptor).channel.map(
                FileChannel.MapMode.READ_ONLY,
                it.startOffset,
                it.declaredLength
            )
        }
    }

    private fun loadVocabulary() {
        context.assets.open("vocab.txt").bufferedReader().useLines { lines ->
            lines.forEachIndexed { index, word ->
                vocabMap[word] = index
            }
        }
    }

    fun generateEmbedding(text: String): FloatArray {
        val tokens = tokenize(text)
        val inputArray = Array(1) { IntArray(MAX_SEQUENCE_LENGTH) }
        
        // Pad or truncate tokens to fixed length
        tokens.take(MAX_SEQUENCE_LENGTH).forEachIndexed { index, token ->
            inputArray[0][index] = vocabMap[token] ?: UNK_TOKEN_ID
        }

        val outputArray = Array(1) { FloatArray(embeddingSize) }
        interpreter?.run(inputArray, outputArray)
        return outputArray[0]
    }

    private fun tokenize(text: String): List<String> {
        return text.lowercase()
            .replace(Regex("[^a-z0-9 ]"), " ")
            .split(Regex("\\s+"))
            .filter { it.isNotEmpty() }
    }

    companion object {
        private const val MAX_SEQUENCE_LENGTH = 128
        private const val UNK_TOKEN_ID = 1
    }
} 