# Simple Screen Share with chat

## Tests

1. To tests server run: `sbt test`
2. There are no frontend (javascript) tests. Automated tests of frontend using WebRtc are tricky - see
[here](https://blog.andyet.com/2014/09/29/testing-webrtc-applications/). 

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