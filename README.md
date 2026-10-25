# JComics

A small Android reader for the comic sites of the Chinese web. It browses a site in an embedded
browser, opens a comic as a list of chapters and reads the pages in a full screen pager.

| | |
|---|---|
| Package | `com.jsoft.jcomic` |
| Version | 4.9 (`versionCode` 20230609) |
| Android | 5.0 (API 21) and up, compiled against API 31 |
| Toolchain | Kotlin 1.6.21, Android Gradle Plugin 8.13.2, Gradle 8.13 |

## Supported sites

The sites are reached through the buttons on the home screen. A comic is opened by tapping it in
the browser of the site, the app takes over from the chapter list onwards.

| Site | Address | Entry point |
|---|---|---|
| 卡拉漫畫 Kalam | https://www.kalamanhua.com/ | button on the home screen |
| 無限動漫 8Comic | https://www.8comic.com/ | button on the home screen |
| 動漫屋 DM5 | http://www.dm5.com/ | button on the home screen |
| 酷漫屋 KuMan5 | http://kuman5.com/ | parser only, the button is commented out |
| 奇妙動漫 QiMiao | https://m.qimiaomh.com/ | parser only, the button is commented out |
| 哔哩哔哩漫畫 Bilibili Manga | https://manga.bilibili.com/ | parser only, the button is hidden |

The buttons of the hidden and commented out sites can be brought back in
`app/src/main/res/layout/activity_grid_view.xml` - the parsers are still in place.

## Features

* Embedded browser per site, the comic pages of the site are opened in the app.
* Full screen reader: a tap on the right or the left edge of the picture turns the page, a fling
  does the same, a fling up leaves the reader, a fling down shows the slider of the page number,
  a pinch zooms the picture and a double tap zooms in and out. Buttons for the previous and the
  next chapter and a button to reload a page that did not load are part of the reader.
* Bookmarks: a comic can be added to the bookmarks from the chapter list, the home screen shows the
  bookmarked comics with the site they come from, the chapter that was read last is highlighted and
  the page of it is remembered.
* Offline reading: long press a chapter to download it, the downloaded chapters are managed under
  *Downloaded Episode* in the menu of the home screen.
* Five pictures are loaded in parallel and the responses of the sites are kept in a 100 MB cache.
* The downloaded chapters and the cached book data are written to the `jComics` folder of the
  external storage, the bookmarks are kept in the database of the app.

## How the sites are read

Every site has its own pair of parsers, one for the page of the comic and one for the page of a
chapter. They live in `app/src/main/java/com/jsoft/jcomic/praser/`.

* **8Comic** rejects requests that do not look like a browser, so all requests use a browser user
  agent. The site is served by more than one host - the app rewrites the urls to the host that
  answers and migrates the bookmarks of the old host. A comic without chapters on the site itself
  cannot be shown by the app.
* **Kalam** puts an access check (*访问验证*) in front of every page. The check writes a token into
  the cookie `omo_token`, the app reads it and sends it with the following requests. The pictures
  live on a second host that only answers to a request with that cookie and a referer of the
  site. The tokens are short lived, the app gets a new one when the site asks for it.
* **Bilibili Manga** only hands out its data through a signed api, only the javascript of the site
  can read it. The app therefore runs the javascript of the site in a web view outside of the
  screen and takes the chapter list from the state of the page. A guest only sees the preview
  chapters, the rest needs a login of the site - the button of the site is hidden for that reason.

## Building

The project builds with the Gradle wrapper that is part of the repository.

* JDK 17 or newer, the build was tested with JDK 21.
* Android SDK with platform 31 installed.

```bash
# Linux / macOS
./gradlew :app:assembleDebug

# Windows
gradlew.bat :app:assembleDebug
```

The apk is written to `app/build/outputs/apk/debug/app-debug.apk`. Android Studio can open the
project as it is, the wrapper takes care of the Gradle version.

## Project layout

```
app/src/main/java/com/jsoft/jcomic/
├── GridViewActivity.kt      home screen: bookmarks, buttons of the sites, browser
├── EpisodeListActivity.kt   chapter list of a comic, bookmarks, download
├── FullscreenActivity.kt    reader: pager, chapter switching, page slider
├── DownloadListActivity.kt  downloaded chapters
├── adapter/                 adapters of the grids, the chapter list and the reader
├── helper/                  bookmarks, downloader, http and image helpers, site helpers
└── praser/                  one book parser and one episode parser per site
```

## Notes

* The sites change their markup from time to time. When a site stops working, the parser of the
  site in `praser/` has to be updated.
* The log of the app is written with the tag `jComics`, it shows which page of a site was read and
  why a chapter was not loaded.

## Disclaimer

This app is meant for personal use. It only reads the pages that the sites serve publicly, the
comics belong to their authors and publishers.
