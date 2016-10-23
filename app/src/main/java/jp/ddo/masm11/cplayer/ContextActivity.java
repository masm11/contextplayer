package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.Intent;
import android.content.DialogInterface;
import android.widget.ArrayAdapter;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.EditText;
import android.widget.Button;
import android.view.View;
import android.app.AlertDialog;

import java.util.List;
import java.util.LinkedList;

public class ContextActivity extends AppCompatActivity {
    private class Datum {
	public long id;
	public String name;
	@Override
	public String toString() {
	    return name;
	}
    }
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_context);
	
	ListView listView = (ListView) findViewById(R.id.context_list);
	assert listView != null;
	
	List<Datum> data = new LinkedList<Datum>();
	for (PlayContext ctxt: PlayContext.all()) {
	    Datum datum = new Datum();
	    datum.id = ctxt.getId();
	    datum.name = ctxt.name;
	    data.add(datum);
	}
	
	ArrayAdapter<Datum> adapter = new ArrayAdapter<>(this, android.R.layout.simple_list_item_1, data);
	
	listView.setAdapter(adapter);
	
	listView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    @Override
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		Datum data = (Datum) listView.getItemAtPosition(position);
		
		PlayContext ctxt = PlayContext.find(data.id);
		Intent intent = new Intent(ContextActivity.this, PlayerService.class);
		intent.setAction("SWITCH");
		intent.putExtra("CONTEXT_ID", ctxt.getId());
		startService(intent);
	    }
	});
	
	listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
	    @Override
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		final ArrayAdapter<Datum> adapter = (ArrayAdapter<Datum>) parent.getAdapter();
		final Datum datum = (Datum) listView.getItemAtPosition(position);
		
		String[] menuitems = new String[] { "Edit", "Delete" };
		AlertDialog.Builder builder = new AlertDialog.Builder(ContextActivity.this);
		builder.setItems(menuitems, new DialogInterface.OnClickListener() {
		    @Override
		    public void onClick(DialogInterface dialog, int which) {
			AlertDialog.Builder builder;
			switch (which) {
			case 0:	// edit
			    final EditText editText = new EditText(ContextActivity.this);
			    editText.setText(datum.name);
			    builder = new AlertDialog.Builder(ContextActivity.this);
			    builder.setTitle("Edit the context name");
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
			    break;
			    
			case 1:	// delete
			    if (adapter.getCount() >= 2) {
				builder = new AlertDialog.Builder(ContextActivity.this);
				builder.setMessage("Are you sure to delete it?");
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
				builder.setMessage("Can't delete the last context.");
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
		builder.setTitle("Name the new context");
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
			ctxt.topDir = "/sdcard/Music";
			ctxt.save();
			
			Datum datum = new Datum();
			datum.id = ctxt.getId();
			datum.name = newName;
			
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
