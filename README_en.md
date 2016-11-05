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

  To access external storage.

- android.permission.BLUETOOTH

  To stop playing when you turn off the bluetooth headset.

Environments I use:

- Xperia X Performance (au SOV33)
- Android 6.0.1

## Usage

First, the app requests permission to access external storage.
You need to grant it.

You see the main screen.

- Main screen

  - Selected context name

    A "context" has:

      - the name
      - the top folder name to play
      - the name of the playing file
      - the position in the playing file

    When you tap the context name, you see the Context List screen.

  - Playing file information

    The path, title, and artist of the playing file are displayed.
    In a file path, the top folder is gray-backed.

    When you tap this area, you see the Explorer screen.

  - Seek bar

    You see the position of the playing file. You can slide to seek.

  - Buttons

    There are Previous, Play, Pause, and Next buttons.
    When you tap the Previous button, the play position will back to the head
    of the playing file if the current position >= 3sec, otherwise to the head
    of the previous file.

- Context List screen

  - List

    You see the list of the contexts in the order of created time.
    You can tap one of them to switch to it and to resume playing.
    You can long tap to edit the context name or to delete one.

  - Buttons

    You can create a new context.

- Explorer screen

  - path

    You see the folder path. The top folder of the path is gray-backed.

  - List

    - Directories

      You see the directory name. You can tap one to go to it, and you can
      long tap to change the top folder to it.

    - Regular files

      You see the filename, the file type, the title, and the artist.
      You can tap one to play it. If it can't be played, the app will
      skip it and play the next one.

- Play order

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
- timed words
- CDDB
- shuffle

You can try to convince if you want new features.
I may implements them if I feel like that,
although I will implements ones I want.

If you find bugs, I'll fix them. Probably.
I have found a bug:
- the titles may be corrupted

## License

GPLv3. See COPYING3.

This app uses Activeandroid, whose license is Apache License 2.0.
See LICENSE-2.0.txt.

## Author

Yuuki Harano &lt;masm@masm11.ddo.jp&gt;
