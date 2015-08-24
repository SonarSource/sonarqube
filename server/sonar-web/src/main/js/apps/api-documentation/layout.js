define([
  'backbone.marionette',
  '../../components/common/jquery-isolated-scroll',
  './templates'
], function (Marionette) {

  return Marionette.LayoutView.extend({
    template: Templates['api-documentation-layout'],

    regions: {
      headerRegion: '.search-navigator-workspace-header',
      actionsRegion: '.search-navigator-filters',
      resultsRegion: '.api-documentation-results',
      detailsRegion: '.search-navigator-workspace-details'
    },

    onRender: function () {
      var navigator = this.$('.search-navigator');
      navigator.addClass('sticky search-navigator-extended-view');
      var top = navigator.offset().top;
      this.$('.search-navigator-workspace-header').css({ top: top });
      this.$('.search-navigator-side').css({ top: top }).isolatedScroll();
    }
  });

});
