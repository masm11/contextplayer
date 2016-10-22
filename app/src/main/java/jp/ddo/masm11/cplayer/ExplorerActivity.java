package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.media.MediaMetadataRetriever;
import android.webkit.MimeTypeMap;
import android.content.Intent;

import java.io.File;
import java.util.ArrayList;

public class ExplorerActivity extends AppCompatActivity {
    private static MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    
    public static boolean isAudioType(String mimeType) {
	if (mimeType == null)
	    return false;
	if (mimeType.startsWith("audio/"))
	    return true;
	if (mimeType.equals("application/ogg"))
	    return true;
	return false;
    }
    
    private static class FileItem {
	private File file;
	private String title;
	private String artist;
	private String mimeType;
	public FileItem(File file) {
	    this.file = file;
	    if (!file.isDirectory()) {
		String ext = mimeTypeMap.getFileExtensionFromUrl(file.toURI().toString());
		android.util.Log.d("ExplorerActivity", "ext=" + ext);
		mimeType = mimeTypeMap.getMimeTypeFromExtension(ext);
		android.util.Log.d("ExplorerActivity", "mimeType=" + mimeType);
		if (isAudioType(mimeType)) {
		    MediaMetadataRetriever retr = new MediaMetadataRetriever();
		    try {
			retr.setDataSource(file.getAbsolutePath());
			title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
			artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		    } catch (Exception e) {
			android.util.Log.i("ExplorerActivity", "exception", e);
		    }
		    retr.release();
		}
	    }
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
	public String getMimeType() {
	    return mimeType;
	}
	public File getFile() {
	    return file;
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
	    View view;
	    String str;
	    
	    if (!item.isDir()) {
		textView = (TextView) convertView.findViewById(R.id.filename);
		assert textView != null;
		str = item.getFilename();
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
		
		view = convertView.findViewById(R.id.for_file);
		assert view != null;
		view.setVisibility(View.VISIBLE);
		
		view = convertView.findViewById(R.id.for_dir);
		assert view != null;
		view.setVisibility(View.GONE);
	    } else {
		textView = (TextView) convertView.findViewById(R.id.dirname);
		assert textView != null;
		str = item.getFilename() + "/";
		textView.setText(str);
		
		view = convertView.findViewById(R.id.for_file);
		assert view != null;
		view.setVisibility(View.GONE);
		
		view = convertView.findViewById(R.id.for_dir);
		assert view != null;
		view.setVisibility(View.VISIBLE);
	    }
	    
	    return convertView;
	}
    }
    
    private File rootDir;	// /sdcard/Music これより上には戻れない
    private File topDir;
    private File curDir;
    private FileAdapter adapter;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
	
	adapter = new FileAdapter(this, new ArrayList<FileItem>());
	
	rootDir = new File("/sdcard/Music");
	topDir = rootDir;
	renewAdapter(topDir);
	
	ListView listView = (ListView) findViewById(R.id.list);
	assert listView != null;
	listView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		FileItem item = (FileItem) listView.getItemAtPosition(position);
		android.util.Log.d("ExplorerActivity", "clicked=" + item.getFilename());
		
		if (item.isDir()) {
		    File dir = item.getFile();
		    if (item.getFilename().equals(".."))
			dir = curDir.getParentFile();
		    renewAdapter(dir);
		} else {
		    play(item.getFile());
		}
	    }
	});
	listView.setOnItemLongClickListener(new ListView.OnItemLongClickListener() {
	    public boolean onItemLongClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		FileItem item = (FileItem) listView.getItemAtPosition(position);
		android.util.Log.d("ExplorerActivity", "longclicked=" + item.getFilename());
		
		if (item.isDir()) {
		    File dir = item.getFile();
		    if (item.getFilename().equals(".."))
			dir = curDir.getParentFile();
		    setTopDir(dir);
		    return true;
		} else
		    return false;
	    }
	});
    }
    
    private void setTopDir(File newDir) {
	topDir = newDir;
	
	// topDir からの相対で curDir を表示
	String topPath = topDir.toString();
	String curPath = curDir.toString();
	String relPath = curPath;
	if (curPath.startsWith(topPath)) {
	    relPath = curPath.substring(topPath.length());
	    if (relPath.startsWith("/"))
		relPath = relPath.substring(1);
	    if (relPath.length() == 0)
		relPath = ".";
	    relPath += "/";
	}
	TextView textView = (TextView) findViewById(R.id.path);
	assert textView != null;
	textView.setText(relPath);
    }
    
    private void renewAdapter(File newDir) {
	File[] files = newDir.listFiles();
	ArrayList<FileItem> items = new ArrayList<FileItem>();
	for (int i = 0; i < files.length; i++) {
	    android.util.Log.d("ContextPlayer", files[i].toString());
	    items.add(new FileItem(files[i]));
	}
	// fixme: sort
	
	android.util.Log.d("ExplorerActivity", "newDir=" + newDir.toString());
	android.util.Log.d("ExplorerActivity", "rootDir=" + rootDir.toString());
	if (!newDir.equals(rootDir))
	    items.add(0, new FileItem(new File(newDir, "..")));
	
	adapter.clear();
	adapter.addAll(items);
	
	ListView listView = (ListView) findViewById(R.id.list);
	assert listView != null;
	listView.setAdapter(adapter);
	
	// topDir からの相対で newDir を表示
	String topPath = topDir.toString();
	String newPath = newDir.toString();
	String relPath = newPath;
	if (newPath.startsWith(topPath)) {
	    relPath = newPath.substring(topPath.length());
	    if (relPath.startsWith("/"))
		relPath = relPath.substring(1);
	    if (relPath.length() == 0)
		relPath = ".";
	    relPath += "/";
	}
	TextView textView = (TextView) findViewById(R.id.path);
	assert textView != null;
	textView.setText(relPath);
	
	curDir = newDir;
    }
    
    private void play(File file) {
	Intent intent = new Intent(this, PlayerService.class);
	intent.setAction("PLAY");
	intent.putExtra("path", file.getAbsolutePath());
	startService(intent);
    }
}
