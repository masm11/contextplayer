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
package jp.ddo.masm11.contextplayer.ui;

import android.support.v7.widget.Toolbar;
import android.app.Fragment;
import android.app.Service;
import android.app.AlertDialog;
import android.webkit.WebView;
import android.webkit.WebSettings;
import android.webkit.JavascriptInterface;
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

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.IOException;

import jp.ddo.masm11.contextplayer.R;

import jp.ddo.masm11.logger.Log;

public class ActionBarFragment extends Fragment {
    private class WebAppInterface {
	private Context context;
	public WebAppInterface(Context context) {
	    this.context = context;
	}
	@JavascriptInterface
	public String getAppVersion() {
	    try {
		PackageManager pm = context.getPackageManager();
		PackageInfo pi = pm.getPackageInfo("jp.ddo.masm11.contextplayer", 0);
		return pi.versionName;
	    } catch (PackageManager.NameNotFoundException e) {
		Log.e("namenotfoundexception", e);
	    }
	    return "???";
	}
    }
    
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
	    LayoutInflater inflater = getActivity().getLayoutInflater();
	    
	    WebView webView = (WebView) inflater.inflate(R.layout.about_dialog, null);
	    WebSettings settings = webView.getSettings();
	    settings.setJavaScriptEnabled(true);
	    webView.addJavascriptInterface(new WebAppInterface(context), "android");
	    webView.loadUrl(getResources().getString(R.string.about_url));
	    
	    builder.setView(webView);
	    
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
    
    private String readFile(int id) {
	InputStream is = null;
	Reader r = null;
	try {
	    is = getResources().openRawResource(id);
	    r = new InputStreamReader(is);
	    StringBuilder sb = new StringBuilder();
	    char[] buf = new char[1024];
	    int s;
	    while ((s = r.read(buf)) != -1)
		sb.append(buf, 0, s);
	    return sb.toString();
	} catch (IOException e) {
	    Log.e("ioexception", e);
	} finally {
	    if (r != null) {
		try {
		    r.close();
		} catch (IOException e) {
		    Log.e("ioexception", e);
		}
	    }
	    if (is != null) {
		try {
		    is.close();
		} catch (IOException e) {
		    Log.e("ioexception", e);
		}
	    }
	}
	
	return "";
    }
    
    public Toolbar getToolbar() {
	return toolbar;
    }
}
