# Context Player

## Feature

This app is an audio player for Android smartphones.

There are many player apps with "Playlists", but most of those can't restore
the paused position.

Also, I use folder structure as playlists, and I want for players to play
in order of path. But there are few such ones.

There is such a player, which is named "Music Folder Player" (de.zorillasoft.musicfolderplayer),
but it can't play files in subdirectories recursively.

This Context Player app have such features.
Although, only those are important, so other features are not implemented.

## Installation

I don't use Google Play now, so install this app with Android Studio.

It uses these permissions:

- android.permission.READ_EXTERNAL_STORAGE

  to access external storage.

- android.permission.BLUETOOTH

  to stop playing when you turn off the bluetooth headset.

Environments I use:

- Xperia X Performance (au SOV33)
- Android 6.0.1

## Contexts

The following information is saved as a context.

  - context name
  - top folder to play
  - playing filename
  - position in the playing filename

## Usage

Some points which you may not understand by seeing are here:

- Main screen

  You can tap the context name and the music information.

- Explorer screen

  You can long tap a directory to set it as the top directory to play.

  You can long tap the back key to back to the main screen.

## Play order

The app lists up all the files under the top folder recursively, and
play them in the order of the path.

There are exceptions:
- Files and directories whose name starts with `.` are ignored.
- Case insensitive.

The app plays files if it can play, even if the type of a file is not shown
in Explorer.

## Miscellaneous

- Bluetooth headsets

  When you turn of the bluetooth headset, the app stop playing.

- Music file places

  I don't know well but the directory to place the audio files is fixed
  per phone model, and apps can get it with this API.
```
    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_MUSIC);
```
  For example, it is `/storage/emulated/0/Music` in my case.
  The app finds files in it, so you need to place audio files in it.

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

This app uses Activeandroid, whose license is Apache License 2.0.
See LICENSE-2.0.txt.

## Author

Yuuki Harano &lt;masm@masm11.ddo.jp&gt;
