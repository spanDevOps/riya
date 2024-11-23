@Singleton
class CommunicationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val ttsService: TtsService,
    private val contactsManager: ContactsManager
) {
    private val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE) as TelephonyManager
    private val callStateListener = object : PhoneStateListener() {
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            super.onCallStateChanged(state, phoneNumber)
            when (state) {
                TelephonyManager.CALL_STATE_RINGING -> {
                    phoneNumber?.let { number ->
                        announceIncomingCall(number)
                    }
                }
            }
        }
    }

    init {
        telephonyManager.listen(callStateListener, PhoneStateListener.LISTEN_CALL_STATE)
    }

    suspend fun sendWhatsAppMessage(contactNameOrNumber: String, message: String): Result<Unit> {
        return try {
            val contact = contactsManager.findContact(contactNameOrNumber)
                ?: return Result.failure(Exception("Contact not found"))

            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = Uri.parse("https://api.whatsapp.com/send?phone=${contact.phoneNumber}&text=${Uri.encode(message)}")
                `package` = "com.whatsapp"
            }
            
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun makePhoneCall(contactNameOrNumber: String): Result<Unit> {
        return try {
            val contact = contactsManager.findContact(contactNameOrNumber)
                ?: return Result.failure(Exception("Contact not found"))

            val intent = Intent(Intent.ACTION_CALL).apply {
                data = Uri.parse("tel:${contact.phoneNumber}")
            }
            
            context.startActivity(intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun announceIncomingCall(phoneNumber: String) {
        scope.launch {
            val contact = contactsManager.findContactByNumber(phoneNumber)
            val name = contact?.name ?: "Unknown caller"
            ttsService.speak("Incoming call from $name")
        }
    }
} 