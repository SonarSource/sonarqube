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

    onClose: function () {
      ModalForm.prototype.onClose.apply(this, arguments);
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest();
    }

  });

});
