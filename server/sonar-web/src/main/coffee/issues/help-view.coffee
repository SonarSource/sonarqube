#$dialog
#.dialog({
#      dialogClass: "no-close",
#      width: width,
#      draggable: false,
#      autoOpen: false,
#      modal: true,
#      minHeight: 50,
#      resizable: false,
#      title: null,
#      close: function () {
#      $j('#modal').remove();
#}
#});

define [
  'common/modals'
  'templates/issues'
], (
  ModalView
  Templates
) ->


  class extends ModalView
    template: Templates['issues-help']
