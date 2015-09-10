define([
  './templates'
], function () {

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
