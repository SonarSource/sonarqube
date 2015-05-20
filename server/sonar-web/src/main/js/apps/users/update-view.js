define([
  'components/common/modal-form',
  './templates'
], function (ModalForm) {

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
      var that = this;
      this.model.set({
        name: this.$('#create-user-name').val(),
        email: this.$('#create-user-email').val(),
        scmAccounts: this.getScmAccounts()
      });
      this.disableForm();
      return this.model.save(null, {
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
