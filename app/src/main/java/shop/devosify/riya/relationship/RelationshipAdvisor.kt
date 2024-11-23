@Singleton
class RelationshipAdvisor @Inject constructor(
    private val contactsManager: ContactsManager,
    private val communicationAnalyzer: CommunicationAnalyzer,
    private val memoryDao: MemoryDao
) {
    suspend fun analyzeRelationships() {
        val communications = communicationAnalyzer.getRecentCommunications()
        val relationships = analyzeRelationshipPatterns(communications)
        suggestRelationshipMaintenance(relationships)
    }

    suspend fun suggestSocialActivities() {
        val friends = contactsManager.getCloseFriends()
        val activities = generateSocialSuggestions(friends)
        presentSocialSuggestions(activities)
    }
} 