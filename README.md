# CSFileTransferAndroid

## How to use:

### Add dependencies:
Add maven { url 'https://jitpack.io' } in your root build.gradle at the end of repositories:
```
allprojects {
		repositories {
			...
			maven { url 'https://jitpack.io' }
		}
	}
```
Add the dependency:
```
implementation 'com.github.cloverstudio:CSFileTransferAndroid:v1.2'
```

### Add permissions in manifest:
```
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.INTERNET" />
```

### configure CSUpload object:
```Java
    private CSUpload csUpload;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        csUpload = CSUpload.getInstance(
	    "http://localhost:3000/upload", //server
	    1024 * 1024, // size of chunk in bytes, up to 10 * 1024 * 1024
	    5, // number of connections, between 1 and 5 
	    getContentResolver()); //content resolver
	)
        
    }
```

### upload a file:
```Java
SingleFile singleFile = csUpload.upload(uri)	//upload function returns singleFile object, a file currently uploading, you can also add another argument, url that overrides the previously set url on CSUpload only for current file
```

### singleFile functions:
```Java
singleFile.pause()	//pauses upload of a file
singleFile.continue()	//continues upload of a file
```

### progress listener:
```Java
singleFile.setOnProgressListener(new OnProgressListener() {
	@Override
	public void onProgress(long max, int uploaded) {
		progressBar.setMax((int)max);
                progressBar.setProgress(uploaded);
            }
        });
```
### error listener:
```Java
csUpload.setOnServerListener(new OnServerListener() {
            @Override
            public void onFailedConnection() {
                new AlertDialog.Builder(MainActivity.this)
                        .setTitle("Can't Connect To Server!")
                        .setMessage("")
                        .show();
            }
        });
```
