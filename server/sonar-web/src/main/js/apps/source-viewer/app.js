define([
  'backbone.marionette',
  'components/source-viewer/main'
], function (Marionette, SourceViewer) {

  var App = new Marionette.Application(),
      init = function (options) {
        this.addRegions({ mainRegion: options.el });

        var viewer = new SourceViewer();
        this.mainRegion.show(viewer);
        viewer.open(options.component.uuid);
        if (window.line) {
          viewer.on('loaded', function () {
            viewer
                .highlightLine(window.line)
                .scrollToLine(window.line);
          });
        }
      };

  App.on('start', function (options) {
    if (options.component) {
      init.call(App, options);
    }
  });

  return App;

});
