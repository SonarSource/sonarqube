import $ from 'jquery';
import Marionette from 'backbone.marionette';

const EVENT_SCOPE = 'modal';

export default Marionette.ItemView.extend({
  className: 'modal',
  overlayClassName: 'modal-overlay',
  htmlClassName: 'modal-open',

  events: function () {
    return {
      'click .js-modal-close': 'onCloseClick'
    };
  },

  onRender: function () {
    var that = this;
    this.$el.detach().appendTo($('body'));
    $('html').addClass(this.htmlClassName);
    this.renderOverlay();
    this.keyScope = key.getScope();
    key.setScope('modal');
    key('escape', 'modal', function () {
      that.destroy();
      return false;
    });
    this.show();
    if (this.options.large) {
      this.$el.addClass('modal-large');
    }
  },

  show: function () {
    var that = this;
    setTimeout(function () {
      that.$el.addClass('in');
      $('.' + that.overlayClassName).addClass('in');
    }, 0);
  },

  onDestroy: function () {
    $('html').removeClass(this.htmlClassName);
    this.removeOverlay();
    key.deleteScope('modal');
    key.setScope(this.keyScope);
  },

  onCloseClick: function (e) {
    e.preventDefault();
    this.destroy();
  },

  renderOverlay: function () {
    var overlay = $('.' + this.overlayClassName);
    if (overlay.length === 0) {
      $('<div class="' + this.overlayClassName + '"></div>').appendTo($('body'));
    }
  },

  removeOverlay: function () {
    $('.' + this.overlayClassName).remove();
  },

  attachCloseEvents: function () {
    var that = this;
    $('body').on('click.' + EVENT_SCOPE, function () {
      $('body').off('click.' + EVENT_SCOPE);
      that.destroy();
    });
  }
});
