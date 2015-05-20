define([
  './templates'
], function () {

  return Marionette.Layout.extend({
    template: Templates['users-layout'],

    regions: {
      headerRegion: '#users-header',
      searchRegion: '#users-search',
      listRegion: '#users-list',
      listFooterRegion: '#users-list-footer'
    }
  });

});
