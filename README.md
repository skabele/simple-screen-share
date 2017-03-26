# Simple Screen Share with chat

## Usage

Simple Screen Share is minimal app, which is intended to run on machine one wants to share screen. Once run, you can
open it on assigned port (default is 9000). Connection from localhost will share its screen, other connections are for
clients. There is also global chat for all participants.

## Requirements

**On sharing machine:**

* Requires Java Runtime Environment 1.8 (version 1.7 _may_ work too).
* Latest Chrome browser with extension enabling screen sharing.

**On clients:**

* Any WebRTC capable browser (tested on Chrome and Firefox). No extension needed.

## Tests

1. To tests server run: `sbt test`
2. There are no frontend (javascript) tests. Automated tests of frontend using WebRtc are tricky - see
[here](https://blog.andyet.com/2014/09/29/testing-webrtc-applications/).

## Build

This is standard sbt project, so run `sbt compile` to compile and `sbt run` to start. Project is also configured with
[sbt-assembly](https://github.com/sbt/sbt-assembly). Run `sbt assembly` to package to single
`.jar` file. Then you can run it by `java -jar target/scala-2.11/simple-screen-share.jar`. 

## Chrome extension

On machine from which you will share you need Chrome extension. Until it is published, it can be installed by cloning
[repository](https://github.com/skabele/screensharing-extensions) and
[manually loading](https://developer.chrome.com/extensions/getstarted#unpacked) extension
`screensharing-extensions/chrome-screensharing-extension` to Chrome.

## Javascript

Javascript dependencies are handled via [bower](https://bower.io/) using config file `public/bower.json`.
Installed files are stored in `public/bower_components` and versioned with project.
To update bower components run:
``` bash
cd public && bower update
```
  
Notice: there are ways to integrate handling JS dependencies with sbt ([1](https://github.com/sbt/sbt-web),
[2](https://github.com/lbialy/play-ng2-webpack2)), but for this small project I chose simple solution.

Javascript logging is using [loglevel](https://github.com/pimterry/loglevel) wrapper. To change log level to debug,
run in javascript console: 
``` js
log.setLevel('debug')
```

## Sources & similar projects

* Official [WebRtc codelab](https://codelabs.developers.google.com/codelabs/webrtc-web/)
* Muaz Khan's [WebRTC-Experiment](https://github.com/muaz-khan/WebRTC-Experiment)
* [Chrome screensharing extension](https://github.com/bistri/screensharing-extensions) used to enable screen sharing
* [Screen sharing](https://github.com/ArseniKavalchuk/share-the-screen) in just 300 lines of code using Motion JPEG over HTTP