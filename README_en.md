# Context Player

## Feature

This app is an audio player for Android smartphones.

There are many player apps with "Playlists", but most of them can't restore
the paused position.

Also, I use folder structure as playlists, and I want for player apps to play
in order of path. But there are few such apps.

There is such a player, which is named "Music Folder Player" (de.zorillasoft.musicfolderplayer),
but it can't play files in subdirectories recursively.

This Context Player app have such features.
Although, only those are important, so other features are not implemented.

## Installation

I don't use Google Play now, so build and install this app with Android Studio by yourself.

It uses these permissions:

- android.permission.READ_EXTERNAL_STORAGE

  to access internal and secondary storages.

- android.permission.BLUETOOTH

  to stop playing when you turn off the bluetooth headset.

- android.permission.INTERNET

  for Crashlytics.

- com.android.launcher.permission.INSTALL_SHORTCUT

  to create shortcut icons on the home screen.

Environments I use:

- Xperia X Performance (au SOV33, Japan)
- Android 8.0

Android 8.0 or later is required.

## Contexts

The following information is saved as a context.

  - context name
  - top folder to play
  - playing filename
  - position in the playing filename
  - volume

## Usage

Here are some points which you may not understand at a glance.

- Main screen

  You can tap the context name and the music information.

- Explorer screen

  You can long tap a directory to set it as the top directory to play.

  You can long tap the back key to back to the main screen.

- Volume slider

  You can adjust volume between 50-100%. It changes not the media volume,
  but the ContextPlayer volume.

  You can adjust only 50%-. Because you rarely changes to less than 50%
  (if you want to do so, you should change the media volume), and I want
  to keep resolution high.

## Play order

The app lists up all the files under the top folder recursively, and
play them in the order of the path.

There are some exceptions:
- Files and directories whose name starts with `.` are ignored.
- Case insensitive.

The app plays files if it can play them, even if the type of a file is not shown
in Explorer.

## Miscellaneous

- Bluetooth headsets

  When you turn off the bluetooth headset, the app stop playing.

- Where you should place music files

  If you want to use the internal storage, place them in the model-specific
  directory, e.g. `/storage/emulated/0/Music`.

  If you want to use the secondary storage, what directory the secondary
  storage is mounted is model-specific. It is `/storage/<UUID>` for me.
  The app supports `/storage/<UUID>`, so I can place music files in
  `/storage/<UUID>/Music`. For other models, if it mounts secondary storage
  on `/storage/<UUID>`, then you can use it, otherwise you can't.

## New features and bugs

I am not going to add new features, e.g.

- albumarts
- equalizer
- timed text
- CDDB
- shuffle

You can try to convince if you want new features.
I may implements them if I feel like that,
although I will implements ones I want.

If you find bugs, I'll fix them. Probably.

## License

GPLv3. See COPYING3.

## Author

Yuuki Harano &lt;masm+github@masm11.me&gt;
