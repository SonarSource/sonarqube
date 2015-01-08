requirejs.config({
  baseUrl: baseUrl + '/js'
});

requirejs([
  'source-viewer/viewer'

], function (SourceViewer) {

  var App = new Marionette.Application();

  App.addRegions({
    viewerRegion: '#source-viewer'
  });

  App.addInitializer(function () {
    var viewer = new SourceViewer();
    App.viewerRegion.show(viewer);
    viewer.open(window.file.uuid);
    if (typeof window.file.line === 'number') {
      viewer.on('loaded', function () {
        viewer
            .highlightLine(window.file.line)
            .scrollToLine(window.file.line);
      });
    }
  });

  var l10nXHR = window.requestMessages();

  l10nXHR.done(function () {
    App.start();
  });

});
