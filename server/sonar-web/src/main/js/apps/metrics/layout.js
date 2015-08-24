define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['metrics-layout'],

    regions: {
      headerRegion: '#metrics-header',
      listRegion: '#metrics-list',
      listFooterRegion: '#metrics-list-footer'
    }
  });

});
