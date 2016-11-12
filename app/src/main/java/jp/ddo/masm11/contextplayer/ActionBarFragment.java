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

import android.support.v7.widget.Toolbar;
import android.app.Fragment;
import android.app.Service;
import android.app.AlertDialog;
import android.widget.ImageButton;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MenuInflater;
import android.os.Bundle;
import android.os.IBinder;
import android.content.Context;
import android.content.Intent;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.pm.PackageInfo;

public class ActionBarFragment extends Fragment {
    private Toolbar toolbar;
    
    @Override
    public void onCreate(Bundle savedInstanceState) {
	Log.d("");
	super.onCreate(savedInstanceState);
	
	setHasOptionsMenu(true);
    }
    
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
	View view = inflater.inflate(R.layout.actionbar_fragment, container, false);
	
	toolbar = (Toolbar) view.findViewById(R.id.toolbar);
	
	return view;
    }
    
    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
	inflater.inflate(R.menu.actionbar, menu);
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
	switch (item.getItemId()) {
	case R.id.action_about:
	    Context context = getContext();
	    AlertDialog.Builder builder = new AlertDialog.Builder(context);
	    PackageManager pm = context.getPackageManager();
	    String ver = "???";
	    try {
		PackageInfo pi = pm.getPackageInfo("jp.ddo.masm11.contextplayer", 0);
		ver = pi.versionName;
	    } catch (PackageManager.NameNotFoundException e) {
		Log.e(e, "namenotfoundexception");
	    }
	    builder.setMessage(getResources().getString(R.string.about_this_app, ver));
	    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		public void onClick(DialogInterface dialog, int which) {
		    // NOP
		}
	    });
	    builder.show();
	    return true;
	    
	default:
	    return super.onOptionsItemSelected(item);
	}
    }
    
    public Toolbar getToolbar() {
	return toolbar;
    }
}
