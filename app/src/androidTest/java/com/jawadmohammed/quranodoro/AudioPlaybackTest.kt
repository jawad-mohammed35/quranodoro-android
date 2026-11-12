package com.jawadmohammed.quranodoro

import android.content.ComponentName
import androidx.lifecycle.Lifecycle
import androidx.media3.common.MediaItem
import androidx.media3.session.MediaController
import androidx.media3.session.SessionToken
import androidx.test.core.app.ActivityScenario
import androidx.test.platform.app.InstrumentationRegistry
import com.jawadmohammed.quranodoro.audio.QuranService
import com.jawadmohammed.quranodoro.data.reciters
import com.google.common.util.concurrent.ListenableFuture
import org.junit.Assert.*
import org.junit.Assume.assumeTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

/** Opt-in network integration test; ordinary CI stays deterministic and offline. */
class AudioPlaybackTest {
    @Test fun streamedAudioContinuesInBackgroundAndWithScreenOff() {
        val instrumentation=InstrumentationRegistry.getInstrumentation()
        assumeTrue(InstrumentationRegistry.getArguments().getString("networkAudio")=="true")
        val context=instrumentation.targetContext
        val scenario=ActivityScenario.launch(MainActivity::class.java)
        lateinit var future: ListenableFuture<MediaController>
        instrumentation.runOnMainSync {future=MediaController.Builder(context,SessionToken(context,ComponentName(context,QuranService::class.java))).buildAsync()}
        val player=future.get(15,TimeUnit.SECONDS)
        fun main(block: ()->Unit)=instrumentation.runOnMainSync(block)
        fun position():Long {var p=0L;main{p=player.currentPosition};return p}
        try {
            main {player.setMediaItem(MediaItem.fromUri(reciters.first().url(1)));player.prepare();player.play()}
            val deadline=System.currentTimeMillis()+30000
            var playing=false
            while(!playing&&System.currentTimeMillis()<deadline){main{playing=player.isPlaying};Thread.sleep(100)}
            assertTrue("MP3 should decode and start playback",playing)
            val start=position()
            scenario.moveToState(Lifecycle.State.CREATED)
            instrumentation.uiAutomation.executeShellCommand("input keyevent 223").close()
            Thread.sleep(2500)
            val end=position()
            assertTrue("Playback should advance while activity is stopped and screen is off",end-start>1500)
            main{player.pause()}
            val paused=position();Thread.sleep(600);assertTrue(kotlin.math.abs(position()-paused)<200)
            main{player.seekTo(5000);player.play()}
            Thread.sleep(700);assertTrue(position()>=5000)
            main{player.stop()}
        } finally {
            instrumentation.uiAutomation.executeShellCommand("input keyevent 224").close()
            instrumentation.uiAutomation.executeShellCommand("input keyevent 82").close()
            main{MediaController.releaseFuture(future)}
            scenario.close()
        }
    }
}
