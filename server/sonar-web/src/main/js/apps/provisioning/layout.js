define([
  './templates'
], function () {

  return Marionette.Layout.extend({
    template: Templates['provisioning-layout'],

    regions: {
      headerRegion: '#provisioning-header',
      searchRegion: '#provisioning-search',
      listRegion: '#provisioning-list',
      listFooterRegion: '#provisioning-list-footer'
    }
  });

});
