/*
 * Sonara - AI-powered audio equalizer
 * Copyright (C) 2024-2026 Sonara
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.sonara.app.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.media.audiofx.AudioEffect
import com.sonara.app.ai.SonaraAi
import com.sonara.app.ai.bridge.AudioSessionTracker

class AudioEffectSessionReceiver : BroadcastReceiver() {
    companion object { @Volatile var bridgeCallback: ((String, Int, String?) -> Unit)? = null }
    override fun onReceive(ctx: Context?, intent: Intent?) {
        val action = intent?.action ?: return
        // Validate: only accept known audio effect actions
        if (action != AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION &&
            action != AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION) return
        val sid = intent.getIntExtra(AudioEffect.EXTRA_AUDIO_SESSION, -1)
        val pkg = intent.getStringExtra(AudioEffect.EXTRA_PACKAGE_NAME)
        // VULN-07: Validate package name exists and belongs to an installed app
        if (pkg != null && ctx != null) {
            try { ctx.packageManager.getPackageInfo(pkg, 0) } catch (_: Exception) { return }
        }
        if (sid > 0) {
            bridgeCallback?.invoke(intent.action ?: "", sid, pkg)
            when (intent.action) {
                AudioEffect.ACTION_OPEN_AUDIO_EFFECT_CONTROL_SESSION -> {
                    AudioSessionTracker.set(sid)
                    SonaraAi.getInstance()?.onSessionChanged(sid)
                }
                AudioEffect.ACTION_CLOSE_AUDIO_EFFECT_CONTROL_SESSION -> {
                    AudioSessionTracker.clear(sid)
                }
            }
        }
    }
}
