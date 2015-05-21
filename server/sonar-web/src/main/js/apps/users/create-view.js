define([
  './user',
  './form-view'
], function (User, FormView) {

  return FormView.extend({

    sendRequest: function () {
      var that = this,
          user = new User({
            login: this.$('#create-user-login').val(),
            name: this.$('#create-user-name').val(),
            email: this.$('#create-user-email').val(),
            password: this.$('#create-user-password').val(),
            scmAccounts: this.getScmAccounts()
          });
      this.disableForm();
      return user.save(null, {
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.collection.refresh();
        that.close();
      }).fail(function (jqXHR) {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
    }
  });

});
