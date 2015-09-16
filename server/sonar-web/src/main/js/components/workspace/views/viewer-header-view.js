define([
  '../templates'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    template: Templates['workspace-viewer-header'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'mousedown .js-resize': 'onResizeClick',

      'click .js-minimize': 'onMinimizeClick',
      'click .js-full-screen': 'onFullScreenClick',
      'click .js-normal-size': 'onNormalSizeClick',
      'click .js-close': 'onCloseClick'
    },

    onRender: function () {
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
      this.$('.js-normal-size').addClass('hidden');
    },

    onDestroy: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
      $('.tooltip').remove();
    },

    onResizeClick: function (e) {
      e.preventDefault();
      this.startResizing(e);
    },

    onMinimizeClick: function (e) {
      e.preventDefault();
      this.trigger('viewerMinimize');
    },

    onFullScreenClick: function (e) {
      e.preventDefault();
      this.toFullScreen();
    },

    onNormalSizeClick: function (e) {
      e.preventDefault();
      this.toNormalSize();
    },

    onCloseClick: function (e) {
      e.preventDefault();
      this.trigger('viewerClose');
    },

    startResizing: function (e) {
      this.initialResizePosition = e.clientY;
      this.initialResizeHeight = $('.workspace-viewer-container').height();
      var processResizing = _.bind(this.processResizing, this),
          stopResizing = _.bind(this.stopResizing, this);
      $('body')
          .on('mousemove.workspace', processResizing)
          .on('mouseup.workspace', stopResizing);
    },

    processResizing: function (e) {
      var currentResizePosition = e.clientY,
          resizeDelta = this.initialResizePosition - currentResizePosition,
          height = this.initialResizeHeight + resizeDelta;
      $('.workspace-viewer-container').height(height);
    },

    stopResizing: function () {
      $('body')
          .off('mousemove.workspace')
          .off('mouseup.workspace');
    },

    toFullScreen: function () {
      this.$('.js-normal-size').removeClass('hidden');
      this.$('.js-full-screen').addClass('hidden');
      this.initialResizeHeight = $('.workspace-viewer-container').height();
      $('.workspace-viewer-container').height('9999px');
    },

    toNormalSize: function () {
      this.$('.js-normal-size').addClass('hidden');
      this.$('.js-full-screen').removeClass('hidden');
      $('.workspace-viewer-container').height(this.initialResizeHeight);
    }
  });

});
