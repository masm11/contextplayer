/* Context Player - Audio Player with Contexts
    Copyright (C) 2016, 2018 Yuuki Harano

    This program is free software: you can redistribute it and/or modify
    it under the terms of the GNU General Public License as published by
    the Free Software Foundation, either version 3 of the License, or
    (at your option) any later version.

    This program is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with this program.  If not, see <http://www.gnu.org/licenses/>.
*/
package me.masm11.contextplayer.widget

import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetManager
import android.widget.RemoteViews
import android.content.Context
import android.content.Intent
import android.content.ComponentName
import android.app.PendingIntent
import android.os.Bundle

import me.masm11.contextplayer.R
import me.masm11.contextplayer.ui.ContextActivity
import me.masm11.contextplayer.service.PlayerService
import me.masm11.contextplayer.db.AppDatabase
import me.masm11.contextplayer.db.PlayContext
import me.masm11.contextplayer.Application

import me.masm11.logger.Log

class WidgetProvider : AppWidgetProvider() {
    override fun onUpdate(context: Context, appWidgetManager: AppWidgetManager, appWidgetIds: IntArray) {
        Log.d("begin.")
        Log.d("appWidgetId.length=${appWidgetIds.size}")
        for (i in appWidgetIds.indices)
            Log.d("appWidgetId=${appWidgetIds[i]}")

	val db = AppDatabase.getDB()

	val playContexts = (context.getApplicationContext() as Application).getPlayContextList()
        val ctxt = playContexts.getCurrent()
        var contextName: String? = null
        if (ctxt != null)
            contextName = ctxt.name

        updateAppWidget(context, appWidgetIds, 0, contextName)

        Log.d("end.")
    }

    override fun onReceive(context: Context, intent: Intent) {
        Log.d("intent=${intent}")
        val action = intent.action
        Log.d("action=${action}")
        val bundle = intent.extras
        if (bundle != null) {
            for (key in bundle.keySet()) {
                val value = bundle.get(key)
                Log.d("key=${key}, value=${value}")
            }
        } else
            Log.d("extras=none")

        super.onReceive(context, intent)
    }

    companion object {
        fun updateAppWidget(context: Context, appWidgetIds: IntArray?,
                            icon: Int, contextName: String?) {
            val appWidgetManager = AppWidgetManager.getInstance(context)
            val ids = appWidgetIds ?: appWidgetManager.getAppWidgetIds(ComponentName(context, WidgetProvider::class.java))

	    if (ids != null) {
		for (appWidgetId in ids) {
		    Log.d("packageName=${context.packageName}")

		    val rv = RemoteViews(context.packageName, R.layout.appwidget)

		    var intent = Intent(context, ContextActivity::class.java)
		    var pendingIntent = PendingIntent.getActivity(context, 0, intent, 0)
		    rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent)

		    intent = Intent(context, PlayerService::class.java)
		    intent.action = PlayerService.ACTION_TOGGLE
		    pendingIntent = PendingIntent.getForegroundService(context, 0, intent, 0)
		    rv.setOnClickPendingIntent(R.id.widget_button, pendingIntent)

		    if (icon != 0)
			rv.setImageViewResource(R.id.widget_button, icon)

		    if (contextName != null)
			rv.setTextViewText(R.id.widget_text, contextName)

		    appWidgetManager.updateAppWidget(appWidgetId, rv)
		}
	    }
        }
    }
}
