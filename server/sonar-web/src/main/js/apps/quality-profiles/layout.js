import Marionette from 'backbone.marionette';
import IntroView from './intro-view';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['quality-profiles-layout'],

  regions: {
    headerRegion: '.search-navigator-workspace-header',
    actionsRegion: '.search-navigator-filters',
    resultsRegion: '.quality-profiles-results',
    detailsRegion: '.search-navigator-workspace-details'
  },

  onRender: function () {
    var navigator = this.$('.search-navigator');
    navigator.addClass('sticky search-navigator-extended-view');
    var top = navigator.offset().top;
    this.$('.search-navigator-workspace-header').css({ top: top });
    this.$('.search-navigator-side').css({ top: top }).isolatedScroll();
    this.renderIntro();
  },

  renderIntro: function () {
    this.detailsRegion.show(new IntroView());
  }
});


