import PopupView from '../../common/popup';

export default PopupView.extend({
  className: 'bubble-popup issue-bubble-popup',

  template: function () {
    return '<div class="bubble-popup-arrow"></div>';
  },

  events: function () {
    return {
      'click .js-issue-form-cancel': 'destroy'
    };
  },

  onRender: function () {
    PopupView.prototype.onRender.apply(this, arguments);
    this.options.view.$el.appendTo(this.$el);
    this.options.view.render();
  },

  onDestroy: function () {
    this.options.view.destroy();
  },

  attachCloseEvents: function () {

  }
});


