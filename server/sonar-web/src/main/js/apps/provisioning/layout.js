define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['provisioning-layout'],

    regions: {
      headerRegion: '#provisioning-header',
      searchRegion: '#provisioning-search',
      listRegion: '#provisioning-list',
      listFooterRegion: '#provisioning-list-footer'
    }
  });

});
