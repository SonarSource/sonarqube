/* https://github.com/ivaynberg/select2/issues/1246 */

;(function($) {

  $.ui.dialog.prototype._allowInteraction = function(e) {
    return !!$(e.target).closest('.ui-dialog, .ui-datepicker, .select2-drop').length;
  };

})(jQuery);
