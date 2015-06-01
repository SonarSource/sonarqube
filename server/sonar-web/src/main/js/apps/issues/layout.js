define([
  './templates'
], function () {

  var $ = jQuery;
  return Marionette.Layout.extend({
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
      $('.search-navigator').addClass('sticky');
      var top = $('.search-navigator').offset().top;
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
      $('.issues').addClass('issues-extended-view');
    },

    hideComponentViewer: function () {
      $('.issues').removeClass('issues-extended-view');
      if (this.scroll != null) {
        $(window).scrollTop(this.scroll);
      }
    },

    showHomePage: function () {
      $('.issues').addClass('issues-home-view');
    },

    hideHomePage: function () {
      $('.issues').removeClass('issues-home-view');
    }
  });

});
