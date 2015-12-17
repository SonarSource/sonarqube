import ModalForm from '../../components/common/modal-form';
import Template from './templates/users-change-password.hbs';

export default ModalForm.extend({
  template: Template,

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
      that.destroy();
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
    });
  }
});


