/* Context Player - Audio Player with Contexts
    Copyright (C) 2016 Yuuki Harano

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
package jp.ddo.masm11.contextplayer.widget;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.widget.RemoteViews;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;
import android.os.Bundle;

import jp.ddo.masm11.contextplayer.R;
import jp.ddo.masm11.contextplayer.ui.ContextActivity;
import jp.ddo.masm11.contextplayer.service.PlayerService;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	android.util.Log.d("WidgetProvider", "onUpdate(): begin.");
	android.util.Log.d("WidgetProvider", "onUpdate(): appWidgetId.length=" + appWidgetIds.length);
	for (int i = 0; i < appWidgetIds.length; i++)
	    android.util.Log.d("WidgetProvider", "onUpdate(): appWidgetId=" + appWidgetIds[i]);
	
	for (int i = 0; i < appWidgetIds.length; i++) {
	    int appWidgetId = appWidgetIds[i];
	    
	    android.util.Log.d("WidgetProvider", "packageName=" + context.getPackageName());
	    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);
	    
	    Intent intent = new Intent(context, ContextActivity.class);
	    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
	    
	    intent = new Intent(context, PlayerService.class);
	    intent.setAction(PlayerService.ACTION_TOGGLE);
	    pendingIntent = PendingIntent.getService(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
	    
	    appWidgetManager.updateAppWidget(appWidgetId, rv);
	}
	
	Intent intent = new Intent(context, PlayerService.class);
	intent.setAction(PlayerService.ACTION_UPDATE_APPWIDGET);
	context.startService(intent);
	
	android.util.Log.d("WidgetProvider", "onUpdate(): end.");
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
	android.util.Log.d("WidgetProvider", "intent=" + intent.toString());
	String action = intent.getAction();
	android.util.Log.d("WidgetProvider", "action=" + (action != null ? action : "null"));
	Bundle bundle = intent.getExtras();
	if (bundle != null) {
	    for (String key: bundle.keySet()) {
		Object val = bundle.get(key);
		android.util.Log.d("WidgetProvider", "key=" + key + ", val=" + (val != null ? val : "null"));
	    }
	} else
	    android.util.Log.d("WidgetProvider", "extras=none");
	
	super.onReceive(context, intent);
    }
}
