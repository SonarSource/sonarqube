import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['issues-layout'],

  regions: {
    filtersRegion: '.search-navigator-filters',
    facetsRegion: '.search-navigator-facets',
    workspaceHeaderRegion: '.search-navigator-workspace-header',
    workspaceListRegion: '.search-navigator-workspace-list',
    workspaceComponentViewerRegion: '.issues-workspace-component-viewer',
    workspaceHomeRegion: '.issues-workspace-home'
  },

  onRender: function () {
    if (this.options.app.state.get('isContext')) {
      this.$(this.filtersRegion.el).addClass('hidden');
    }
    this.$('.search-navigator').addClass('sticky');
    var top = this.$('.search-navigator').offset().top;
    this.$('.search-navigator-workspace-header').css({ top: top });
    this.$('.search-navigator-side').css({ top: top }).isolatedScroll();
  },

  showSpinner: function (region) {
    return this[region].show(new Marionette.ItemView({
      template: _.template('<i class="spinner"></i>')
    }));
  },

  showComponentViewer: function () {
    this.scroll = $(window).scrollTop();
    this.$('.issues').addClass('issues-extended-view');
  },

  hideComponentViewer: function () {
    this.$('.issues').removeClass('issues-extended-view');
    if (this.scroll != null) {
      $(window).scrollTop(this.scroll);
    }
  },

  showHomePage: function () {
    this.$('.issues').addClass('issues-home-view');
  },

  hideHomePage: function () {
    this.$('.issues').removeClass('issues-home-view');
  }
});


