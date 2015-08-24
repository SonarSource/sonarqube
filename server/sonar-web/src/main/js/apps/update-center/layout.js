define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['update-center-layout'],

    regions: {
      headerRegion: '#update-center-header',
      searchRegion: '#update-center-search',
      listRegion: '#update-center-plugins',
      footerRegion: '#update-center-footer'
    }
  });

});
