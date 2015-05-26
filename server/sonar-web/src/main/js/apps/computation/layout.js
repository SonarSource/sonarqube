define([
  './templates'
], function () {

  return Marionette.Layout.extend({
    template: Templates['computation-layout'],

    regions: {
      headerRegion: '#computation-header',
      searchRegion: '#computation-search',
      listRegion: '#computation-list',
      listFooterRegion: '#computation-list-footer'
    }
  });

});
