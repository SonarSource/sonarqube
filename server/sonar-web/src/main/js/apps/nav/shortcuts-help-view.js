define([
  'components/common/modals',
  './templates'
], function (ModalView) {

  return ModalView.extend({
    className: 'modal modal-large',
    template: Templates['nav-shortcuts-help']
  });

});
