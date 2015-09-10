define([
  './templates'
], function () {

  return Marionette.LayoutView.extend({
    template: Templates['users-layout'],

    regions: {
      headerRegion: '#users-header',
      searchRegion: '#users-search',
      listRegion: '#users-list',
      listFooterRegion: '#users-list-footer'
    }
  });

});
