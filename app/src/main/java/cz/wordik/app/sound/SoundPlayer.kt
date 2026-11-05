package cz.wordik.app.sound

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool
import cz.wordik.app.R
import java.util.concurrent.ConcurrentHashMap

/** Zvuky aplikace: správná odpověď, chyba a krátká fanfára při dokončení testu. */
enum class Sound { CORRECT, WRONG, FINISH }

/**
 * Zvuková služba. Odděleno rozhraním, aby šla v testech nahradit. Zvuky nemají vlastní vypínač
 * ani vibrace; hlasitost určuje zařízení.
 */
interface SoundPlayer {
    fun play(sound: Sound)

    fun release() {}
}

/** Nic nehraje (testy, výchozí hodnota). */
object NoSoundPlayer : SoundPlayer {
    override fun play(sound: Sound) {}
}

/** Přehrává přibalené zvuky přes [SoundPool]; všechny se načtou předem při vytvoření. */
class SoundPoolPlayer(context: Context) : SoundPlayer {
    private val pool: SoundPool = SoundPool.Builder()
        .setMaxStreams(3)
        .setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_GAME)
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .build(),
        )
        .build()

    private val loaded: MutableSet<Int> = ConcurrentHashMap.newKeySet()
    private val ids: Map<Sound, Int>

    init {
        pool.setOnLoadCompleteListener { _, sampleId, status -> if (status == 0) loaded += sampleId }
        val app = context.applicationContext
        ids = mapOf(
            Sound.CORRECT to pool.load(app, R.raw.sound_correct, 1),
            Sound.WRONG to pool.load(app, R.raw.sound_wrong, 1),
            Sound.FINISH to pool.load(app, R.raw.sound_finish, 1),
        )
    }

    override fun play(sound: Sound) {
        val id = ids[sound] ?: return
        if (id in loaded) pool.play(id, 1f, 1f, 1, 0, 1f)
    }

    override fun release() {
        pool.release()
    }
}
