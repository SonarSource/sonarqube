define(['backbone.marionette'], function (Marionette) {

  var $ = jQuery,
      EVENT_SCOPE = 'modal';

  return Marionette.ItemView.extend({
    className: 'modal',
    overlayClassName: 'modal-overlay',

    events: function () {
      return {
        'click .js-modal-close': 'close'
      };
    },

    onRender: function () {
      var that = this;
      this.$el.detach().appendTo($('body'));
      this.renderOverlay();
      this.keyScope = key.getScope();
      key.setScope('modal');
      key('escape', 'modal', function () {
        that.close();
        return false;
      });
    },

    onClose: function () {
      this.removeOverlay();
      key.deleteScope('modal');
      key.setScope(this.keyScope);
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
        that.close();
      });
    }
  });

});
