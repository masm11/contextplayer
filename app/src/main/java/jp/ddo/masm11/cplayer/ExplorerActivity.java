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
	private LayoutInflater inflater;
	public FileAdapter(Context context, ArrayList<FileItem> items) {
	    super(context, R.layout.list_explorer, items);
	    inflater = LayoutInflater.from(context);
	}
	
	public View getView(int position, View convertView, ViewGroup parent) {
	    if (convertView == null)
		convertView = inflater.inflate(R.layout.list_explorer, parent, false);
	    
	    FileItem item = getItem(position);
	    TextView textView;
	    String str;
	    
	    textView = (TextView) convertView.findViewById(R.id.filename);
	    assert textView != null;
	    str = item.getFilename();
	    if (item.isDir())
		str = str + "/";
	    textView.setText(str);
	    
	    textView = (TextView) convertView.findViewById(R.id.title);
	    assert textView != null;
	    str = item.getTitle();
	    if (str == null)
		str = "unknown title";
	    textView.setText(str);
	    
	    textView = (TextView) convertView.findViewById(R.id.artist);
	    assert textView != null;
	    str = item.getArtist();
	    if (str == null)
		str = "unknown artist";
	    textView.setText(str);
	    
	    return convertView;
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
