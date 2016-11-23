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
package jp.ddo.masm11.contextplayer;

import android.appwidget.AppWidgetProvider;
import android.appwidget.AppWidgetManager;
import android.widget.RemoteViews;
import android.content.Context;
import android.content.Intent;
import android.app.PendingIntent;

public class WidgetProvider extends AppWidgetProvider {
    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager, int[] appWidgetIds) {
	for (int i = 0; i < appWidgetIds.length; i++) {
	    int appWidgetId = appWidgetIds[i];
	    
	    RemoteViews rv = new RemoteViews(context.getPackageName(), R.layout.appwidget);
	    
	    Intent intent = new Intent(context, ContextActivity.class);
	    PendingIntent pendingIntent = PendingIntent.getActivity(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_text, pendingIntent);
	    
	    intent = new Intent(context, PlayerService.class);
	    intent.setAction(PlayerService.ACTION_TOGGLE);
	    pendingIntent = PendingIntent.getService(context, 0, intent, 0);
	    rv.setOnClickPendingIntent(R.id.widget_button, pendingIntent);
	    
	    appWidgetManager.updateAppWidget(appWidgetId, rv);
	    
	    intent = new Intent(context, PlayerService.class);
	    intent.setAction(PlayerService.ACTION_UPDATE_APPWIDGET);
	    context.startService(intent);
	}
    }
}
