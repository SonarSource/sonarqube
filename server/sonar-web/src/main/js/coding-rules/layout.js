define([
  'templates/coding-rules'
], function () {

  var $ = jQuery;

  return Marionette.Layout.extend({
    template: Templates['coding-rules-layout'],
    topOffset: 30,

    regions: {
      filtersRegion: '.search-navigator-filters',
      facetsRegion: '.search-navigator-facets',
      workspaceHeaderRegion: '.search-navigator-workspace-header',
      workspaceListRegion: '.search-navigator-workspace-list',
      workspaceDetailsRegion: '.search-navigator-workspace-details'
    },

    showDetails: function () {
      this.scroll = $(window).scrollTop();
      $('.search-navigator').addClass('search-navigator-extended-view');
    },


    hideDetails: function () {
      $('.search-navigator').removeClass('search-navigator-extended-view');
      if (this.scroll != null) {
        $(window).scrollTop(this.scroll);
      }
    }

  });

});
