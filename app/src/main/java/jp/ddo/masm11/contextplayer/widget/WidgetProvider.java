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
import android.content.ComponentName;
import android.app.PendingIntent;
import android.os.Bundle;

import jp.ddo.masm11.contextplayer.R;
import jp.ddo.masm11.contextplayer.ui.ContextActivity;
import jp.ddo.masm11.contextplayer.service.PlayerService;

import jp.ddo.masm11.logger.Log;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	Log.d("begin.");
	Log.d("appWidgetId.length=" + appWidgetIds.length);
	for (int i = 0; i < appWidgetIds.length; i++)
	    Log.d("appWidgetId=" + appWidgetIds[i]);
	
	updateAppWidget(context, appWidgetIds, 0, null);
	
	Intent intent = new Intent(context, PlayerService.class);
	intent.setAction(PlayerService.ACTION_UPDATE_APPWIDGET);
	context.startService(intent);
	
	Log.d("end.");
    }
    
    @Override
    public void onReceive(Context context, Intent intent) {
	Log.d("intent=" + intent.toString());
	String action = intent.getAction();
	Log.d("action=" + (action != null ? action : "null"));
	Bundle bundle = intent.getExtras();
	if (bundle != null) {
	    for (String key: bundle.keySet()) {
		Object val = bundle.get(key);
		Log.d("key=" + key + ", val=" + (val != null ? val : "null"));
	    }
	} else
	    Log.d("extras=none");
	
	super.onReceive(context, intent);
    }
    
    public static void updateAppWidget(Context context, int[] appWidgetIds,
	    int icon, String contextName) {
	AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
	
	if (appWidgetIds == null)
	    appWidgetIds = appWidgetManager.getAppWidgetIds(new ComponentName(context, WidgetProvider.class));
	
	for (int i = 0; i < appWidgetIds.length; i++) {
	    int appWidgetId = appWidgetIds[i];
	    
	    Log.d("packageName=" + context.getPackageName());
	    
	    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);
	    
	    Intent intent = new Intent(context, ContextActivity.class);
	    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
	    
	    intent = new Intent(context, PlayerService.class);
	    intent.setAction(PlayerService.ACTION_TOGGLE);
	    pendingIntent = PendingIntent.getService(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
	    
	    if (icon != 0)
		rv.setImageViewResource(R.id.widget_button, icon);
	    
	    if (contextName != null)
		rv.setTextViewText(R.id.widget_text, contextName);
	    
	    appWidgetManager.updateAppWidget(appWidgetId, rv);
	}
    }
}
