define([
  './viewer-header-view'
], function (HeaderView) {

  return Marionette.LayoutView.extend({
    className: 'workspace-viewer',

    modelEvents: {
      'destroy': 'destroy'
    },

    regions: {
      headerRegion: '.workspace-viewer-header',
      viewerRegion: '.workspace-viewer-container'
    },

    onRender: function () {
      this.showHeader();
      this.$('.workspace-viewer-container').isolatedScroll();
    },

    onViewerMinimize: function () {
      this.trigger('viewerMinimize');
    },

    onViewerClose: function () {
      this.trigger('viewerClose', this.model);
    },

    showHeader: function () {
      var headerView = new HeaderView({ model: this.model });
      this.listenTo(headerView, 'viewerMinimize', this.onViewerMinimize);
      this.listenTo(headerView, 'viewerClose', this.onViewerClose);
      this.headerRegion.show(headerView);
    }
  });

});
