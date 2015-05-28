define([
  'components/common/popup'
], function (PopupView) {

  return PopupView.extend({
    className: 'bubble-popup issue-bubble-popup',

    template: function () {
      return '<div class="bubble-popup-arrow"></div>';
    },

    events: function () {
      return {
        'click .js-issue-form-cancel': 'close'
      };
    },

    onRender: function () {
      PopupView.prototype.onRender.apply(this, arguments);
      this.options.view.$el.appendTo(this.$el);
      this.options.view.render();
    },

    onClose: function () {
      this.options.view.close();
    },

    attachCloseEvents: function () {

    }
  });

});
