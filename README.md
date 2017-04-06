# Simple Screen Share with chat

Simple Screen Share is minimal app, which allows to share machine screen (or selected application windows or
single browser tab). Application is run as a minimal server on background. Connection from localhost will share its
screen, other connections are considered clients. There is also global chat for all participants. Under the hood it
uses [WebRTC](https://webrtc.org/).

## Requirements

**On sharing machine:**

* Requires Java Runtime Environment 1.8 (version 1.7 _may_ work too).
* Latest Chrome browser with
  [extension](https://chrome.google.com/webstore/detail/simple-screen-share/ekdnppciaajpkmgcdbilgiacdfildblb) enabling
  screen sharing.

**On clients:**

* Up-to-date WebRTC capable browser (tested on Chrome and Firefox on Linux and Windows 10). No extension needed.
Sharing to Chrome on Android works too, but without external mouse it is not possible to drag & drop view controlls. 

## How to use

1. Download a version or build (see bellow) `simple-screen-share.jar`.
2. Start it using Java 8: `java -jar simple-screen-share.jar`.
3. Open port `localhost:9000` from Google Chrome
4. If you do not have extension installed, you will be asked to do so
5. Click button Share desktop
6. Send your public IP or URL with appended `:9000` to people interested to watch and ask them to open it in
   recent version of Google Chrome or Firefox.

To can change port (for example to default http port `80`) run it with parameter:
```
java -Dhttp.port=80 -jar simple-screen-share.jar
```

## How to develop

Aplication is standard Play 2.5 scala applicatin build by sbt. To start it on custom port with debug logback
config: 
```
sbt run -Dhttp.port=9100 -Dlogger.file=conf/logback.debug.xml
```

### Tests

1. To run backed tests run: `sbt test`
2. There are no frontend (javascript) tests. Automated tests of frontend using WebRtc are
   [tricky](https://blog.andyet.com/2014/09/29/testing-webrtc-applications/).

### Build to single jar

Project is also configured with
[sbt-assembly](https://github.com/sbt/sbt-assembly). Run `sbt assembly` to package to single
`.jar` file `target/scala-2.11/simple-screen-share.jar`. 

### Chrome extension

Google Chrome from security reasons allows screen sharing only via extension. Extension can be installed
[here](https://chrome.google.com/webstore/detail/simple-screen-share/ekdnppciaajpkmgcdbilgiacdfildblb).
Sources are in [skabele/screensharing-extensions](https://github.com/skabele/screensharing-extensions)
(fork of [bistri/screensharing-extensions](https://github.com/bistri/screensharing-extensions), limited
to usage from localhost only).

### Javascript

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

* Official WebRtc [codelab](https://codelabs.developers.google.com/codelabs/webrtc-web/) and
  [code samples](https://github.com/webrtc/samples)
* Muaz Khan's [WebRTC-Experiment](https://github.com/muaz-khan/WebRTC-Experiment)
* [Chrome screensharing extension](https://github.com/bistri/screensharing-extensions) used to enable screen sharing
* [Screen sharing](https://github.com/ArseniKavalchuk/share-the-screen) in just 300 lines of code using Motion JPEG over HTTP