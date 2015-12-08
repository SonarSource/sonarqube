import Marionette from 'backbone.marionette';
import Template from './templates/api-documentation-layout.hbs';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    headerRegion: '.search-navigator-workspace-header',
    actionsRegion: '.search-navigator-filters',
    searchRegion: '.api-documentation-search',
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
