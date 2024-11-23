@Singleton
class ContactsManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    suspend fun findContact(nameOrNumber: String): Contact? = withContext(Dispatchers.IO) {
        val contentResolver = context.contentResolver
        val cursor = contentResolver.query(
            ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
            arrayOf(
                ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                ContactsContract.CommonDataKinds.Phone.NUMBER
            ),
            null,
            null,
            null
        )

        cursor?.use {
            while (it.moveToNext()) {
                val name = it.getString(0)
                val number = it.getString(1)
                
                if (name.contains(nameOrNumber, ignoreCase = true) || 
                    number.contains(nameOrNumber)) {
                    return@withContext Contact(name, number)
                }
            }
        }
        null
    }

    suspend fun findContactByNumber(phoneNumber: String): Contact? = withContext(Dispatchers.IO) {
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(phoneNumber)
        )
        
        val cursor = context.contentResolver.query(
            uri,
            arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
            null,
            null,
            null
        )

        cursor?.use {
            if (it.moveToFirst()) {
                val name = it.getString(0)
                return@withContext Contact(name, phoneNumber)
            }
        }
        null
    }
}

data class Contact(
    val name: String,
    val phoneNumber: String
) 