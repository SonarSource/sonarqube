import Marionette from 'backbone.marionette';
import IntroView from './intro-view';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['quality-gates-layout'],

  regions: {
    headerRegion: '.search-navigator-workspace-header',
    actionsRegion: '.search-navigator-filters',
    resultsRegion: '.quality-gates-results',
    detailsRegion: '.search-navigator-workspace-details'
  },

  onRender: function () {
    var top = this.$('.search-navigator').offset().top;
    this.$('.search-navigator-workspace-header').css({ top: top });
    this.$('.search-navigator-side').css({ top: top }).isolatedScroll();
    this.renderIntro();
  },

  renderIntro: function () {
    this.detailsRegion.show(new IntroView());
  }
});


