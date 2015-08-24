define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['provisioning-form'],

    onRender: function () {
      ModalForm.prototype.onRender.apply(this, arguments);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onDestroy: function () {
      ModalForm.prototype.onDestroy.apply(this, arguments);
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest();
    }

  });

});
