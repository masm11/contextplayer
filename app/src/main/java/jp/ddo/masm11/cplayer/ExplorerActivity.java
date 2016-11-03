package jp.ddo.masm11.cplayer;

import android.support.v7.app.AppCompatActivity;
import android.app.Service;
import android.os.Bundle;
import android.os.Handler;
import android.os.Environment;
import android.os.IBinder;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.AdapterView;
import android.content.Context;
import android.view.View;
import android.view.ViewGroup;
import android.view.LayoutInflater;
import android.view.KeyEvent;
import android.media.MediaMetadataRetriever;
import android.webkit.MimeTypeMap;
import android.content.Intent;
import android.content.ServiceConnection;
import android.content.ComponentName;

import java.io.File;
import java.io.FileFilter;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.Locale;

public class ExplorerActivity extends AppCompatActivity {
    private static MimeTypeMap mimeTypeMap = MimeTypeMap.getSingleton();
    private PlayerServiceConnection conn;
    private PlayerService.PlayerServiceBinder svc;
    
    private class PlayerServiceConnection implements ServiceConnection {
	@Override
	public void onServiceConnected(ComponentName name, IBinder service) {
	    svc = (PlayerService.PlayerServiceBinder) service;
	}
	
	@Override
	public void onServiceDisconnected(ComponentName name) {
	    svc = null;
	}
    }
    
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
		Log.d("ext=%s", ext);
		mimeType = mimeTypeMap.getMimeTypeFromExtension(ext);
		Log.d("mimeType=%s", mimeType);
	    }
	}
	public void retrieveMetadata(MediaMetadataRetriever retr) {
	    if (isAudioType(mimeType)) {
		try {
		    retr.setDataSource(file.getAbsolutePath());
		    title = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_TITLE);
		    artist = retr.extractMetadata(MediaMetadataRetriever.METADATA_KEY_ARTIST);
		} catch (Exception e) {
		    Log.i(e, "exception");
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
	@Override
	public int hashCode() {
	    return new StringBuilder()
		    .append(file.toString())
		    .append("\t")
		    .append(title != null ? title : "null")
		    .append("\t")
		    .append(artist != null ? artist : "null")
		    .append("\t")
		    .append(mimeType != null ? mimeType : "null")
		    .hashCode();
	}
    }
    
    private static class FileAdapter extends ArrayAdapter<FileItem> {
	private LayoutInflater inflater;
	public FileAdapter(Context context, ArrayList<FileItem> items) {
	    super(context, R.layout.list_explorer, items);
	    inflater = LayoutInflater.from(context);
	}
	
	@Override
	public boolean hasStableIds() {
	    return true;
	}
	
	@Override
	public long getItemId(int position) {
	    FileItem item = getItem(position);
	    return item.hashCode();
	}
	
	@Override
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
		
		textView = (TextView) convertView.findViewById(R.id.mime_type);
		assert textView != null;
		str = item.getMimeType();
		textView.setText(str);
		
		textView = (TextView) convertView.findViewById(R.id.title);
		assert textView != null;
		str = item.getTitle();
		if (str == null)
		    str = convertView.getContext().getResources().getString(R.string.unknown_title);
		textView.setText(str);
		
		textView = (TextView) convertView.findViewById(R.id.artist);
		assert textView != null;
		str = item.getArtist();
		if (str == null)
		    str = convertView.getContext().getResources().getString(R.string.unknown_artist);
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
    
    private class BackgroundRetriever implements Runnable {
	private LinkedList<FileItem> list;
	private FileAdapter adapter;
	
	public BackgroundRetriever(FileAdapter adapter) {
	    list = new LinkedList<>();
	    this.adapter = adapter;
	}
	
	@Override
	public void run() {
	    MediaMetadataRetriever retr = new MediaMetadataRetriever();
	    try {
		while (true) {
		    FileItem item;
		    
		    synchronized (list) {
			while (list.isEmpty())
			    list.wait();
			item = list.removeFirst();
		    }
		    
		    item.retrieveMetadata(retr);
		    handler.post(new Runnable() {
			@Override
			public void run() {
			    adapter.notifyDataSetChanged();
			}
		    });
		}
	    } catch (InterruptedException e) {
	    }
	    retr.release();
	}
	
	public void setNewItems(ArrayList<FileItem> newList) {
	    synchronized (list) {
		list.clear();
		list.addAll(newList);
		list.notify();
	    }
	}
    }
    
    private File rootDir;	// これより上には戻れない
    private File topDir;
    private File curDir;
    private FileAdapter adapter;
    private PlayContext ctxt;
    private BackgroundRetriever bretr;
    private Thread thread;
    private Handler handler;
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
	Log.init(getExternalCacheDir());
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_explorer);
	
	handler = new Handler();
	
	adapter = new FileAdapter(this, new ArrayList<FileItem>());
	rootDir = Environment.getExternalStoragePublicDirectory(
		Environment.DIRECTORY_MUSIC);
	
	long ctxtId = Long.parseLong(Config.findByKey("context_id").value);
	ctxt = PlayContext.find(ctxtId);
	if (ctxt == null)
	    ctxt = new PlayContext();
	
	bretr = new BackgroundRetriever(adapter);
	thread = new Thread(bretr);
	thread.setPriority(Thread.MIN_PRIORITY);
	thread.start();
	
	topDir = new File(ctxt.topDir);
	renewAdapter(topDir);
	
	ListView listView = (ListView) findViewById(R.id.list);
	assert listView != null;
	listView.setOnItemClickListener(new ListView.OnItemClickListener() {
	    public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
		ListView listView = (ListView) parent;
		FileItem item = (FileItem) listView.getItemAtPosition(position);
		Log.d("clicked=%s", item.getFilename());
		
		if (item.isDir()) {
		    File dir = item.getFile();
		    if (item.getFilename().equals(".."))
			dir = curDir.getParentFile();
		    if (!item.getFilename().equals("."))
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
		Log.d("longclicked=%s", item.getFilename());
		
		if (item.isDir()) {
		    File dir = item.getFile();
		    if (item.getFilename().equals(".."))
			dir = curDir.getParentFile();
		    else if (item.getFilename().equals("."))
			dir = curDir;
		    setTopDir(dir);
		    return true;
		} else
		    return false;
	    }
	});
    }
    
    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
	if (keyCode == KeyEvent.KEYCODE_BACK) {
	    if (!curDir.equals(topDir) && !curDir.equals(rootDir)) {
		renewAdapter(curDir.getParentFile());
		return true;
	    }
	}
	return super.onKeyDown(keyCode, event);
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
	PathView pathView = (PathView) findViewById(R.id.path);
	assert pathView != null;
	pathView.setRootDir(rootDir.toString());
	pathView.setTopDir(topPath);
	pathView.setPath(curPath + "/");
	
	if (svc != null)
	    svc.setTopDir(newDir.getAbsolutePath());
	
	ctxt.topDir = newDir.getAbsolutePath();
	ctxt.path = null;
	ctxt.pos = 0;
	ctxt.save();
    }
    
    /* dir に含まれるファイル名をリストアップする。
     * '.' で始まるものは含まない。
     * ソートされている。
     */
    public static File[] listFiles(File dir, boolean reverse) {
	File[] files = dir.listFiles(new FileFilter() {
	    @Override
	    public boolean accept(File pathname) {
		return !pathname.getName().startsWith(".");
	    }
	});
	
	Comparator<File> comparator = new Comparator<File>() {
	    @Override
	    public int compare(File o1, File o2) {
		String name1 = o1.getName().toLowerCase(Locale.getDefault());
		String name2 = o2.getName().toLowerCase(Locale.getDefault());
		// まず、大文字小文字を無視して比較
		int r = name1.compareTo(name2);
		// もしそれで同じだったら、区別して比較
		if (r == 0)
		    r = o1.compareTo(o2);
		return r;
	    }
	};
	if (reverse)
	    comparator = Collections.reverseOrder(comparator);
	Arrays.sort(files, comparator);
	
	return files;
    }
    
    private void renewAdapter(File newDir) {
	File[] files = listFiles(newDir, false);
	ArrayList<FileItem> items = new ArrayList<FileItem>();
	for (int i = 0; i < files.length; i++) {
	    Log.d("%s", files[i].toString());
	    items.add(new FileItem(files[i]));
	}
	
	Log.d("newDir=%s", newDir.toString());
	Log.d("rootDir=%s", rootDir.toString());
	if (!newDir.equals(rootDir))
	    items.add(0, new FileItem(new File(newDir, "..")));
	else
	    items.add(0, new FileItem(new File(newDir, ".")));
	
	adapter.clear();
	adapter.addAll(items);
	
	bretr.setNewItems(items);
	
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
	PathView pathView = (PathView) findViewById(R.id.path);
	assert pathView != null;
	pathView.setRootDir(rootDir.toString());
	pathView.setPath(newPath + "/");
	pathView.setTopDir(topPath);
	
	curDir = newDir;
    }
    
    private void play(File file) {
	if (svc != null)
	    svc.play(file.getAbsolutePath());
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
    
    @Override
    public void onDestroy() {
	if (thread != null) {
	    thread.interrupt();
	    try {
		thread.join();
	    } catch (InterruptedException e) {
	    }
	    thread = null;
	}
	
	super.onDestroy();
    }
}
