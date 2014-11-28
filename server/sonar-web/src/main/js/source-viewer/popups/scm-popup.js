define([
  'backbone.marionette',
  'templates/source-viewer',
  'common/popup'
], function (Marionette, Templates, Popup) {

  return Popup.extend({
    template: Templates['source-viewer-scm-popup'],

    events: {
      'click': 'onClick'
    },

    onRender: function () {
      Popup.prototype.onRender.apply(this, arguments);
      this.$('.bubble-popup-container').isolatedScroll();
    },

    onClick: function (e) {
      e.stopPropagation();
    }
  });
});
