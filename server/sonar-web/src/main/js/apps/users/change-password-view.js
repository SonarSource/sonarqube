define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

  return ModalForm.extend({
    template: Templates['users-change-password'],

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest();
    },

    sendRequest: function () {
      var that = this,
          password = this.$('#change-user-password-password').val(),
          confirmation = this.$('#change-user-password-password-confirmation').val();
      if (password !== confirmation) {
        that.showErrors([{ msg: 'New password and its confirmation do not match' }]);
        return;
      }
      this.disableForm();
      return this.model.changePassword(password, {
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.close();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});
