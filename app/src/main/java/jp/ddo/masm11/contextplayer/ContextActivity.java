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

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.Parcelable;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
import android.content.ServiceConnection;
import android.content.ComponentName;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.EditText;
import android.widget.Button;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.app.AlertDialog;
import android.app.Service;
import android.app.FragmentManager;
import android.text.InputType;

import java.io.File;
import java.util.List;
import java.util.LinkedList;

public class ContextActivity extends AppCompatActivity {
    private class PlayerServiceConnection implements ServiceConnection {
	private PlayerService.OnStatusChangedListener listener = new PlayerService.OnStatusChangedListener() {
	    @Override
	    public void onStatusChanged(PlayerService.CurrentStatus status) {
		boolean changed = false;
		for (Datum datum: data) {
		    if (datum.id == status.contextId) {
			if (!strEq(datum.path, status.path)) {
			    datum.path = status.path;
			    changed = true;
			}
		    }
		}
		if (changed)
		    adapter.notifyDataSetChanged();
	    }
	};
	
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = (PlayerService.PlayerServiceBinder) service;
	    svc.setOnStatusChangedListener(listener);
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
	    svc = null;
	}
    }
    
    private PlayerServiceConnection conn;
    private PlayerService.PlayerServiceBinder svc;
    private File rootDir;
    private List<Datum> data;
    private DatumAdapter adapter;
    
    private class Datum {
	public long id;
	public String name;
	public String topDir;
	public String path;
    }
    
    private class DatumAdapter extends ArrayAdapter<Datum> {
	private LayoutInflater inflater;
	
	public DatumAdapter(Context context, List<Datum> items) {
	    super(context, R.layout.list_context, items);
	    inflater = LayoutInflater.from(context);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (convertView == null)
		convertView = inflater.inflate(R.layout.list_context, parent, false);
	    
	    Datum item = getItem(position);
	    
	    TextView textView = (TextView) convertView.findViewById(R.id.context_name);
	    assert textView != null;
	    textView.setText(item.name);
	    
	    PathView pathView = (PathView) convertView.findViewById(R.id.context_topdir);
	    assert pathView != null;
	    pathView.setRootDir(rootDir.getAbsolutePath());
	    pathView.setTopDir(item.topDir);
	    pathView.setPath(item.path);
	    
	    return convertView;
	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context);
	
	FragmentManager fragMan = getFragmentManager();
	ActionBarFragment frag = (ActionBarFragment) fragMan.findFragmentById(R.id.actionbar_frag);
	setSupportActionBar(frag.getToolbar());
	
	rootDir = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_MUSIC);
	
	ListView listView = (ListView) findViewById(R.id.context_list);
	assert listView != null;
	
	data = new LinkedList<Datum>();
	for (PlayContext ctxt: PlayContext.all()) {
	    Datum datum = new Datum();
	    datum.id = ctxt.getId();
	    datum.name = ctxt.name;
	    datum.topDir = ctxt.topDir;
	    datum.path = ctxt.path;
	    data.add(datum);
	}
	
	adapter = new DatumAdapter(this, data);
	
	listView.setAdapter(adapter);
	
	listView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		Datum data = (Datum) listView.getItemAtPosition(position);
		
		Config config = Config.findByKey("context_id");
		config.value = "" + data.id;
		config.save();
		
		if (svc != null)
		    svc.switchContext();
	    }
	});
	
	listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
	    @Override
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		@SuppressWarnings("unchecked")
		final ArrayAdapter<Datum> adapter = (ArrayAdapter<Datum>) parent.getAdapter();
		final Datum datum = (Datum) listView.getItemAtPosition(position);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setItems(R.array.context_list_menu, new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			AlertDialog.Builder builder;
			switch (which) {
			case 0:	// edit
			    editContextName(datum);
			    break;
			    
			case 1:	// delete
			    deleteContext(datum);
			    break;
			    
			case 2:	// create icon
			    createIcon(datum);
			    break;
			}
		    }
		});
		builder.show();
		
		return true;
	    }
	    
	    private void editContextName(final Datum datum) {
		final EditText editText = new EditText(ContextActivity.this);
		editText.setInputType(InputType.TYPE_CLASS_TEXT);
		editText.setText(datum.name);
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setTitle(R.string.edit_the_context_name);
		builder.setView(editText);
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			// NOP.
		    }
		});
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			String newName = editText.getText().toString();
			PlayContext ctxt = PlayContext.find(datum.id);
			ctxt.name = newName;
			ctxt.save();
			
			datum.name = newName;
			adapter.notifyDataSetChanged();
		    }
		});
		builder.show();
	    }
	    
	    private void deleteContext(final Datum datum) {
		if (adapter.getCount() >= 2) {
		    AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		    builder.setMessage(R.string.are_you_sure_to_delete_it);
		    builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    // NOP.
			}
		    });
		    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    PlayContext ctxt = PlayContext.find(datum.id);
			    ctxt.delete();
			    adapter.remove(datum);
			}
		    });
		    builder.show();
		} else {
		    AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		    builder.setMessage(R.string.cant_delete_the_last_context);
		    builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
			public void onClick(DialogInterface dialog, int which) {
			    // NOP.
			}
		    });
		    builder.show();
		}
	    }
	    
	    private void createIcon(final Datum datum) {
		final EditText editText = new EditText(ContextActivity.this);
		editText.setInputType(InputType.TYPE_CLASS_TEXT);
		editText.setText(datum.name);
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setTitle(R.string.edit_the_icon_label);
		builder.setView(editText);
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			// NOP.
		    }
		});
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			String label = editText.getText().toString();
			long id = datum.id;
			
			Parcelable icon = Intent.ShortcutIconResource.fromContext(ContextActivity.this, R.drawable.launcher);
			
			Intent intent = new Intent(Intent.ACTION_MAIN);
			intent.setClassName(ContextActivity.this, MainActivity.class.getName());
			intent.putExtra("jp.ddo.masm11.contextplayer.CONTEXT_ID", id);
			
			Intent inst = new Intent("com.android.launcher.action.INSTALL_SHORTCUT");
			inst.putExtra(Intent.EXTRA_SHORTCUT_INTENT, intent);
			inst.putExtra(Intent.EXTRA_SHORTCUT_ICON_RESOURCE, icon);
			inst.putExtra(Intent.EXTRA_SHORTCUT_NAME, label);
			
			sendBroadcast(inst);
		    }
		});
		builder.show();
	    }
	});
	
	Button button = (Button) findViewById(R.id.context_add);
	assert button != null;
	button.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View v) {
		final EditText editText = new EditText(ContextActivity.this);
		editText.setInputType(InputType.TYPE_CLASS_TEXT);
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setTitle(R.string.name_the_new_context);
		builder.setView(editText);
		builder.setNegativeButton(android.R.string.cancel, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			// NOP.
		    }
		});
		builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
		    public void onClick(DialogInterface dialog, int which) {
			String newName = editText.getText().toString();
			PlayContext ctxt = new PlayContext();
			ctxt.name = newName;
			ctxt.topDir = rootDir.getAbsolutePath();
			ctxt.save();
			
			Datum datum = new Datum();
			datum.id = ctxt.getId();
			datum.name = newName;
			datum.topDir = ctxt.topDir;
			
			ListView listView = (ListView) findViewById(R.id.context_list);
			assert listView != null;
			@SuppressWarnings("unchecked")
			ArrayAdapter<Datum> adapter = (ArrayAdapter<Datum>) listView.getAdapter();
			adapter.add(datum);
		    }
		});
		builder.show();
	    }
	});
    }
    
    @Override
    public void onStart() {
	super.onStart();
	
	// started service にする。
	startService(new Intent(this, PlayerService.class));
	
	Intent intent = new Intent(this, PlayerService.class);
	conn = new PlayerServiceConnection();
	bindService(intent, conn, Service.BIND_AUTO_CREATE);
    }
    
    @Override
    public void onStop() {
	unbindService(conn);
	
	super.onStop();
    }
    
    private boolean strEq(String s1, String s2) {
	if (s1 == s2)
	    return true;
	if (s1 == null && s2 != null)
	    return false;
	if (s1 != null && s2 == null)
	    return false;
	return s1.equals(s2);
    }
}
