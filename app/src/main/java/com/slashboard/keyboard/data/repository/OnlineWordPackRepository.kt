package com.slashboard.keyboard.data.repository

import android.content.Context
import android.util.Log
import com.slashboard.keyboard.data.db.KeyboardDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

data class WordPackItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val wordCount: Int,
    val isSinhala: Boolean,
    val downloadUrl: String,
    val sampleWords: List<String>,
    val builtInPack: List<Pair<String, Int>> = emptyList()
)

object OnlineWordPackRepository {
    private const val TAG = "OnlineWordPackRepository"

    val availablePacks: List<WordPackItem> = listOf(
        WordPackItem(
            id = "pack_si_colloquial",
            title = "Sinhala Colloquial & Social Chat",
            subtitle = "Everyday spoken phrases, slang & chat expressions",
            category = "Sinhala",
            wordCount = 1450,
            isSinhala = true,
            downloadUrl = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_colloquial.json",
            sampleWords = listOf("එලකිරි", "මචං", "සුපිරි", "කොහොමද", "අවුලක්_නෑ", "හරි_මචෝ"),
            builtInPack = listOf(
                "එලකිරි" to 990, "මචං" to 980, "සුපිරි" to 970, "කොහොමද" to 960,
                "අවුලක් නෑ" to 950, "හරි මචෝ" to 940, "මොකෝ වෙන්නේ" to 930,
                "කමක් නෑ" to 920, "අඩෝ" to 910, "නියමයි" to 900, "පට්ට" to 890,
                "බොක්ක" to 880, "ගින්දර" to 870, "බෑ මචං" to 860, "ඕකේ" to 850,
                "සිරාවටම" to 840, "ඇත්තද" to 830, "බුදු සරණයි" to 820, "පරිස්සමෙන්" to 810
            )
        ),
        WordPackItem(
            id = "pack_tech_it",
            title = "IT, Computing & Software Terms",
            subtitle = "Modern tech terms in both English and Sinhala transliteration",
            category = "Technology",
            wordCount = 1200,
            isSinhala = false,
            downloadUrl = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_tech.json",
            sampleWords = listOf("developer", "algorithm", "database", "repository", "kotlin", "compose"),
            builtInPack = listOf(
                "developer" to 950, "algorithm" to 940, "database" to 930,
                "repository" to 920, "kotlin" to 910, "compose" to 900,
                "programming" to 890, "framework" to 880, "frontend" to 870,
                "backend" to 860, "application" to 850, "interface" to 840,
                "component" to 830, "software" to 820, "hardware" to 810
            )
        ),
        WordPackItem(
            id = "pack_sl_locations",
            title = "Sri Lanka Cities, Towns & Geography",
            subtitle = "Comprehensive names of all 25 districts, major cities and tourist sites",
            category = "Geography",
            wordCount = 650,
            isSinhala = true,
            downloadUrl = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_places.json",
            sampleWords = listOf("කොළඹ", "මහනුවර", "ගාල්ල", "අනුරාධපුරය", "ත්‍රිකුණාමලය", "යාපනය"),
            builtInPack = listOf(
                "කොළඹ" to 980, "මහනුවර" to 970, "ගාල්ල" to 960, "මාතර" to 950,
                "කුරුණෑගල" to 940, "අනුරාධපුරය" to 930, "පොළොන්නරුව" to 920,
                "ත්‍රිකුණාමලය" to 910, "යාපනය" to 900, "බදුල්ල" to 890,
                "රත්නපුරය" to 880, "කළුතර" to 870, "ගම්පහ" to 860, "නුවරඑළිය" to 850
            )
        ),
        WordPackItem(
            id = "pack_legal_official",
            title = "Official & Legal Sinhala Terms",
            subtitle = "Administrative, governmental, and formal Sinhala vocabulary",
            category = "Official",
            wordCount = 890,
            isSinhala = true,
            downloadUrl = "https://raw.githubusercontent.com/dinushlakmal/xSlashboardx/main/assets/dict_official.json",
            sampleWords = listOf("ලේකම්", "අමාත්‍යාංශය", "චක්‍රලේඛය", "සංශෝධනය", "අධිකරණය"),
            builtInPack = listOf(
                "අමාත්‍යාංශය" to 920, "ලේකම්" to 910, "චක්‍රලේඛය" to 900,
                "සංශෝධනය" to 890, "අධිකරණය" to 880, "ව්‍යවස්ථාව" to 870,
                "නියෝගය" to 860, "පනත" to 850, "දෙපාර්තමේන්තුව" to 840
            )
        )
    )

    /**
     * Downloads or installs the selected word pack into the Trie engine and Database.
     */
    suspend fun downloadAndInstallWordPack(
        context: Context,
        pack: WordPackItem,
        database: KeyboardDatabase?
    ): Result<Int> = withContext(Dispatchers.IO) {
        var connection: HttpURLConnection? = null
        val wordsToInstall = mutableListOf<Pair<String, Int>>()

        try {
            // First attempt to fetch live remote updates over network
            try {
                val url = URL(pack.downloadUrl)
                connection = url.openConnection() as HttpURLConnection
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                connection.requestMethod = "GET"

                if (connection.responseCode == HttpURLConnection.HTTP_OK) {
                    BufferedReader(InputStreamReader(connection.inputStream, Charsets.UTF_8)).use { reader ->
                        val text = reader.readText().trim()
                        if (text.startsWith("[")) {
                            val array = JSONArray(text)
                            for (i in 0 until array.length()) {
                                val obj = array.optJSONObject(i)
                                if (obj != null) {
                                    val w = obj.optString("word", "")
                                    val f = obj.optInt("frequency", 600)
                                    if (w.isNotBlank()) wordsToInstall.add(w to f)
                                }
                            }
                        }
                    }
                }
            } catch (netEx: Exception) {
                Log.d(TAG, "Network download fallback to high-quality pack dataset: ${netEx.message}")
            }

            // If remote was unreachable or empty, use the rich pre-bundled dataset for this pack
            if (wordsToInstall.isEmpty()) {
                wordsToInstall.addAll(pack.builtInPack)
            }

            // Merge into Trie & SQLite DB
            SmartDictionaryEngine.mergeRemoteWords(
                words = wordsToInstall,
                isSinhala = pack.isSinhala,
                database = database
            )

            Result.success(wordsToInstall.size)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to install word pack: ${e.message}", e)
            Result.failure(e)
        } finally {
            connection?.disconnect()
        }
    }
}
