requirejs.config({
  baseUrl: baseUrl + '/js'
});

requirejs([
  'source-viewer/viewer'
], function (SourceViewer) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addRegions({
    viewerRegion: '#source-viewer'
  });

  App.addInitializer(function () {
    $('.js-drilldown-link').on('click', function (e) {
      e.preventDefault();
      var uuid = $(e.currentTarget).data('uuid'),
          viewer = new SourceViewer();
      App.viewerRegion.show(viewer);
      viewer.open(uuid);
      if (window.drilldown.period != null) {
        viewer.on('loaded', function () {
          viewer.filterLinesByDate(window.drilldown.period);
        });
      }
    });
  });

  var l10nXHR = window.requestMessages();
  l10nXHR.done(function () {
    App.start();
  });
});
