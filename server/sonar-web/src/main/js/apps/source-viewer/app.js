define([
  'components/source-viewer/main'
], function (SourceViewer) {

  var App = new Marionette.Application(),
      init = function (options) {
        this.addRegions({ mainRegion: options.el });

        var viewer = new SourceViewer();
        this.mainRegion.show(viewer);
        viewer.open(options.file.uuid);
        if (typeof options.file.line === 'number') {
          viewer.on('loaded', function () {
            viewer
                .highlightLine(options.file.line)
                .scrollToLine(options.file.line);
          });
        }
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  return App;

});
