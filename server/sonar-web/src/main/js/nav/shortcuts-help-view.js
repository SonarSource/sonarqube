define([
  'common/modals',
  'templates/nav'
], function (ModalView) {

  return ModalView.extend({
    template: Templates['nav-shortcuts-help']
  });

});
