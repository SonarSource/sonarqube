define([
  './templates'
], function () {

  return Marionette.Layout.extend({
    template: Templates['groups-layout'],

    regions: {
      headerRegion: '#groups-header',
      searchRegion: '#groups-search',
      listRegion: '#groups-list',
      listFooterRegion: '#groups-list-footer'
    }
  });

});
