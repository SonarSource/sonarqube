define([
  'components/common/modal-form',
  './user',
  './templates'
], function (ModalForm, User) {

  var $ = jQuery;

  return ModalForm.extend({
    template: Templates['users-form'],

    events: function () {
      return _.extend(ModalForm.prototype.events.apply(this, arguments), {
        'click #create-user-add-scm-account': 'onAddScmAccountClick'
      });
    },

    onFormSubmit: function () {
      ModalForm.prototype.onFormSubmit.apply(this, arguments);
      this.sendRequest();
    },

    onAddScmAccountClick: function (e) {
      e.preventDefault();
      this.addScmAccount();
    },

    getScmAccounts: function () {
      var scmAccounts = this.$('[name="scmAccounts"]').map(function () {
        return $(this).val();
      }).toArray();
      return scmAccounts.filter(function (value) {
        return !!value;
      });
    },

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
    },

    addScmAccount: function () {
      var fields = this.$('[name="scmAccounts"]');
      fields.first().clone().val('').insertAfter(fields.last());
    }
  });

});
