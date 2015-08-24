define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['groups-layout'],

    regions: {
      headerRegion: '#groups-header',
      searchRegion: '#groups-search',
      listRegion: '#groups-list',
      listFooterRegion: '#groups-list-footer'
    }
  });

});
