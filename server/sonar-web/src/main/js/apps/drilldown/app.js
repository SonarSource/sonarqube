define([
  'backbone.marionette',
  'components/source-viewer/main'
], function (Marionette, SourceViewer) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {
        App.addRegions({ viewerRegion: '#source-viewer' });
        $('.js-drilldown-link').on('click', function (e) {
          e.preventDefault();
          $(e.currentTarget).closest('table').find('.selected').removeClass('selected');
          $(e.currentTarget).closest('tr').addClass('selected');
          var uuid = $(e.currentTarget).data('uuid'),
              viewer = new SourceViewer();
          App.viewerRegion.show(viewer);
          viewer.open(uuid);
          if (window.drilldown.period != null) {
            viewer.on('loaded', function () {
              viewer.filterLinesByDate(window.drilldown.period, window.drilldown.periodName);
            });
          }
        });
      };

  App.on('start', function (options) {
    init.call(App, options);
  });

  return App;
});
