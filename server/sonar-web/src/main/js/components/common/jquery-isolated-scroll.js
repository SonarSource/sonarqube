(function ($) {

  $.fn.isolatedScroll = function () {
      this.on('wheel', function (e) {
        var delta = -e.originalEvent.deltaY;
        var bottomOverflow = this.scrollTop + $(this).outerHeight() - this.scrollHeight >= 0;
        var topOverflow = this.scrollTop <= 0;
        if ((delta < 0 && bottomOverflow) || (delta > 0 && topOverflow)) {
          e.preventDefault();
        }
      });
  };

})(window.jQuery);
