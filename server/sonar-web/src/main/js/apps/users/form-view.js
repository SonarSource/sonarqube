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

    addScmAccount: function () {
      var fields = this.$('[name="scmAccounts"]');
      fields.first().clone().val('').insertAfter(fields.last());
    }
  });

});
