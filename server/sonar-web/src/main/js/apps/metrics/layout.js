define([
  './templates'
], function () {

  return Marionette.Layout.extend({
    template: Templates['metrics-layout'],

    regions: {
      headerRegion: '#metrics-header',
      listRegion: '#metrics-list',
      listFooterRegion: '#metrics-list-footer'
    }
  });

});
