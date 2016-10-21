package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;

import java.io.File;
import java.util.ArrayList;

public class ExplorerActivity extends AppCompatActivity {
    private static class FileItem {
	private File file;
	private String title;
	private String artist;
	public FileItem(File file) {
	    this.file = file;
	}
	public boolean isDir() {
	    return file.isDirectory();
	}
	public String getTitle() {
	    return title;
	}
	public String getArtist() {
	    return artist;
	}
	public String getFilename() {
	    return file.getName();
	}
    }
    
    private static class FileAdapter extends ArrayAdapter<FileItem> {
	public FileAdapter(Context context, ArrayList<FileItem> items) {
	    super(context, R.layout.list_explorer, items);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
	    LayoutInflater inflater = (LayoutInflater) getContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
	    View view = inflater.inflate(R.layout.list_explorer, parent, false);
	    TextView textView = (TextView) view.findViewById(R.id.text);
	    assert textView != null;
	    textView.setText("foo");
	    
	    return view;
	}
    }
    
    private File rootDir;	// /sdcard/Music これより上には戻れない
    private File curDir;
    private FileAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
	
	adapter = new FileAdapter(this, new ArrayList<FileItem>());
	
	rootDir = new File("/sdcard/Music");
	renewAdapter(rootDir);
    }
    
    private void renewAdapter(File newDir) {
	File[] files = newDir.listFiles();
	ArrayList<FileItem> items = new ArrayList<FileItem>();
	for (int i = 0; i < files.length; i++) {
	    android.util.Log.d("ContextPlayer", files[i].toString());
	    items.add(new FileItem(files[i]));
	}
	// fixme: sort
	
	adapter.clear();
	adapter.addAll(items);
	
	ListView listView = (ListView) findViewById(R.id.list);
	assert listView != null;
	listView.setAdapter(adapter);
	
	curDir = newDir;
    }
}
