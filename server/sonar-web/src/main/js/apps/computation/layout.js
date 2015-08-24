define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['computation-layout'],

    regions: {
      headerRegion: '#computation-header',
      searchRegion: '#computation-search',
      listRegion: '#computation-list',
      listFooterRegion: '#computation-list-footer'
    }
  });

});
