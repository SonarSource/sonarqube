import ModalForm from 'components/common/modal-form';
import './templates';

export default ModalForm.extend({
  template: Templates['users-change-password'],

  onFormSubmit: function (e) {
    this._super(e);
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


