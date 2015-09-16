define([
  '../templates'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'workspace-nav-item',
    template: Templates['workspace-item'],

    modelEvents: {
      'change': 'render',
      'showViewer': 'onViewerShow',
      'hideViewer': 'onViewerHide'
    },

    events: {
      'click': 'onClick',
      'click .js-close': 'onCloseClick'
    },

    onClick: function (e) {
      e.preventDefault();
      this.options.collectionView.trigger('click', this.model);
    },

    onCloseClick: function (e) {
      e.preventDefault();
      e.stopPropagation();
      this.model.destroy();
    },

    onViewerShow: function () {
      this.$el.addClass('hidden');
    },

    onViewerHide: function () {
      this.$el.removeClass('hidden');
    }
  });

});
