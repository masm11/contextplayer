package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.os.Environment;
import android.content.Intent;
import android.content.Context;
import android.content.DialogInterface;
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

import java.io.File;
import java.util.List;
import java.util.LinkedList;

public class ContextActivity extends AppCompatActivity {
    private File rootDir;
    
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
	
	rootDir = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_MUSIC);
	
	ListView listView = (ListView) findViewById(R.id.context_list);
	assert listView != null;
	
	List<Datum> data = new LinkedList<Datum>();
	for (PlayContext ctxt: PlayContext.all()) {
	    Datum datum = new Datum();
	    datum.id = ctxt.getId();
	    datum.name = ctxt.name;
	    datum.topDir = ctxt.topDir;
	    datum.path = ctxt.path;
	    data.add(datum);
	}
	
	DatumAdapter adapter = new DatumAdapter(this, data);
	
	listView.setAdapter(adapter);
	
	listView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		Datum data = (Datum) listView.getItemAtPosition(position);
		
		Config config = Config.findByKey("context_id");
		config.value = "" + data.id;
		config.save();
		
		Intent intent = new Intent(ContextActivity.this, PlayerService.class);
		intent.setAction("SWITCH");
		startService(intent);
	    }
	});
	
	listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
	    @Override
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		final ArrayAdapter<Datum> adapter = (ArrayAdapter<Datum>) parent.getAdapter();
		final Datum datum = (Datum) listView.getItemAtPosition(position);
		
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setItems(R.array.context_list_menu, new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			AlertDialog.Builder builder;
			switch (which) {
			case 0:	// edit
			    final EditText editText = new EditText(ContextActivity.this);
			    editText.setText(datum.name);
			    builder = new AlertDialog.Builder(ContextActivity.this);
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
				    ctxt.topDir = rootDir.getAbsolutePath();
				    ctxt.save();
				    
				    datum.name = newName;
				    datum.topDir = ctxt.topDir;
				    datum.path = null;
				    adapter.notifyDataSetChanged();
				}
			    });
			    builder.show();
			    break;
			    
			case 1:	// delete
			    if (adapter.getCount() >= 2) {
				builder = new AlertDialog.Builder(ContextActivity.this);
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
				builder = new AlertDialog.Builder(ContextActivity.this);
				builder.setMessage(R.string.cant_delete_the_last_context);
				builder.setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
				    public void onClick(DialogInterface dialog, int which) {
					// NOP.
				    }
				});
				builder.show();
			    }
			    break;
			}
		    }
		});
		builder.show();
		
		return true;
	    }
	});
	
	Button button = (Button) findViewById(R.id.context_add);
	assert button != null;
	button.setOnClickListener(new View.OnClickListener() {
	    public void onClick(View v) {
		final EditText editText = new EditText(ContextActivity.this);
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
			ArrayAdapter<Datum> adapter = (ArrayAdapter<Datum>) listView.getAdapter();
			adapter.add(datum);
		    }
		});
		builder.show();
	    }
	});
    }
}
