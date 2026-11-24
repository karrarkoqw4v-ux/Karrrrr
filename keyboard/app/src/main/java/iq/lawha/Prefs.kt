package iq.lawha

import android.content.Context

enum class Skin { WATER, IOS_LIGHT, IOS_DARK }

object Prefs {
    private const val FILE = "lawha"
    private const val KEY_SKIN = "skin"

    fun getSkin(ctx: Context): Skin {
        val sp = ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE)
        return when (sp.getString(KEY_SKIN, Skin.IOS_DARK.name)) {
            Skin.WATER.name -> Skin.WATER
            Skin.IOS_LIGHT.name -> Skin.IOS_LIGHT
            else -> Skin.IOS_DARK
        }
    }

    fun setSkin(ctx: Context, skin: Skin) {
        ctx.getSharedPreferences(FILE, Context.MODE_PRIVATE).edit().putString(KEY_SKIN, skin.name).apply()
    }
}
