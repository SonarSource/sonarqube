define(function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    className: 'bubble-popup',

    onRender: function () {
      this.$el.detach().appendTo($('body'));
      if (this.options.bottom) {
        this.$el.addClass('bubble-popup-bottom');
        this.$el.css({
          top: this.options.triggerEl.offset().top + this.options.triggerEl.outerHeight(),
          left: this.options.triggerEl.offset().left
        });
      } else if (this.options.bottomRight) {
        this.$el.addClass('bubble-popup-bottom-right');
        this.$el.css({
          top: this.options.triggerEl.offset().top + this.options.triggerEl.outerHeight(),
          right: $(window).width() - this.options.triggerEl.offset().left - this.options.triggerEl.outerWidth()
        });
      } else {
        this.$el.css({
          top: this.options.triggerEl.offset().top,
          left: this.options.triggerEl.offset().left + this.options.triggerEl.outerWidth()
        });
      }
      this.attachCloseEvents();
    },

    attachCloseEvents: function () {
      var that = this;
      key('escape', function () {
        that.destroy();
      });
      $('body').on('click.bubble-popup', function () {
        $('body').off('click.bubble-popup');
        that.destroy();
      });
      this.options.triggerEl.on('click.bubble-popup', function (e) {
        that.options.triggerEl.off('click.bubble-popup');
        e.stopPropagation();
        that.destroy();
      });
    },

    onDestroy: function () {
      $('body').off('click.bubble-popup');
      this.options.triggerEl.off('click.bubble-popup');
    }
  });

});
