# コンテキストプレイヤー

For English, see [README_en.md](README_en.md).

## 特徴

Android スマホ用の音楽再生プレイヤーです。

世の中、プレイリスト対応プレイヤーは数多くありますが、
別のプレイリストに行って戻った時、前回の再生位置から再生してくれるものは、
そうはありません。

また、フォルダ構成をプレイリスト代わりにしており、ファイルの path 順に
再生して欲しいのですが、これもなかなか対応しているアプリは少ないです。

こんな感じの機能を持ったアプリとして、
日本語名「音楽フォルダプレイヤー」(de.zorillasoft.musicfolderplayer)がありますが、
これはフォルダ内を再帰的に再生してくれず、私の好みに合いませんでした。

この「コンテキストプレイヤー」は、そういった機能を備えています。
ただし、そこに重点を置き、他の機能はできるだけ削減しています。

## インストール

今のところ、Google Play には上げていませんので、Android Studio 等で
ビルドし、インストールしてください。

以下の権限を使用します。
- android.permission.READ_EXTERNAL_STORAGE

  内部ストレージ及び外付け SD カードにアクセスするため。

- android.permission.BLUETOOTH

  Bluetooth ヘッドセットが OFF されたら再生を停止するため。

- android.permission.INTERNET

  Crashlytics が使うため。

- com.android.launcher.permission.INSTALL_SHORTCUT

  ホーム画面にショートカットアイコンを作成するため。

動作確認環境:
- Xperia X Performance (au SOV33)
- Android 7.0

Android 7.0 以上でないと動かないはずです。

## コンテキストとは

以下の情報を保持します。

  - コンテキストの名前
  - 再生するフォルダ階層のトップ
  - 再生中のファイル名
  - 再生中の位置
  - ボリューム

## 使い方

見た目で解りにくい点だけ説明します。

- メイン画面

  コンテキスト名部分と曲情報部分もタップできます。

- Explorer 画面

  ディレクトリをロングタップすると、そのディレクトリを「再生するフォルダ
  階層のトップ」として設定します。

  back キーをロングタップするとメイン画面に戻ります。

- ボリューム調整

  50〜100% の間でボリュームを調整できます。メディアのボリュームはそのままに、
  コンテキストプレイヤーのボリュームだけが変化します。

  50%〜 となっているのは、50%未満にしたくなることがまずない(大きく変えたいなら
  メディアのボリュームを変更することを推奨)のと、50〜100% の間の解像度を高く
  するためです。

## 再生順

トップフォルダ以下、再帰的に全てのファイルをリストアップし、
path でソートして、その順に再生します。

ただし、以下の例外があります。
- `.` で始まるファイルやフォルダは無視
- 大文字小文字は無視

また、Explorer でファイルの種類が表示されていなくても、再生できるなら
再生しています。

## その他

- Bluetooth ヘッドセット

  OFF すると再生が止まります。

- 曲ファイルを置く場所

  内蔵ストレージ内に置くなら、機種ごとに音楽ファイルが決まっていますので、
  そこに置いて下さい。
  例えば私の環境では `/storage/emulated/0/Music` のようです。

  また、外付け SD カードの場合、外付け SD カードがどこに mount されるかが
  機種により異なります。私の機種では `/storage/<UUID>` に mount されます。
  `/storage/<UUID>` には対応しているので、`/storage/<UUID>/Music` にも置けます。
  他の機種の場合、`/storage/<UUID>` に mount されるなら使えますが、そうでない場合は
  外付け SD カードは使えません。

## 機能追加及びバグについて

これ以上機能を追加する気はほとんどありません。
例えば、以下のような機能を追加する予定は全くありません。

- albumart
- equalizer
- 歌詞
- CDDB
- shuffle

まぁもしどうしても欲しい機能があれば、私を説得してみてください。
基本的に私が欲しい機能しか作るつもりはありませんが、
気が向けば実装するかもしれません。

バグについては修正します。たぶん。きっと。
しかし、Shift-JIS な曲名が化けるのは仕様です。

## ライセンス

GPLv3 です。COPYING3 を参照してください。

このアプリは Activeandroid を使用しています。
Activeandroid のライセンスは Apache License 2.0 です。
LICENSE-2.0.txt を参照してください。

## 作者

Yuuki Harano &lt;masm@masm11.ddo.jp&gt;
