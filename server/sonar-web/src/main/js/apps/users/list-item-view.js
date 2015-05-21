define([
  './update-view',
  './change-password-view',
  './deactivate-view',
  './templates'
], function (UpdateView, ChangePasswordView, DeactivateView) {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'panel panel-vertical',
    template: Templates['users-list-item'],

    events: {
      'click .js-user-update': 'onUpdateClick',
      'click .js-user-change-password': 'onChangePasswordClick',
      'click .js-user-deactivate': 'onDeactivateClick'
    },

    onRender: function () {
      this.$el.attr('data-login', this.model.id);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onUpdateClick: function (e) {
      e.preventDefault();
      this.updateUser();
    },

    onChangePasswordClick: function (e) {
      e.preventDefault();
      this.changePassword();
    },

    onDeactivateClick: function (e) {
      e.preventDefault();
      this.deactivateUser();
    },

    updateUser: function () {
      new UpdateView({
        model: this.model,
        collection: this.model.collection
      }).render();
    },

    changePassword: function () {
      new ChangePasswordView({
        model: this.model,
        collection: this.model.collection
      }).render();
    },

    deactivateUser: function () {
      new DeactivateView({ model: this.model }).render();
    },

    serializeData: function () {
      var scmAccounts = this.model.get('scmAccounts'),
          scmAccountsLimit = scmAccounts.length > 3 ? 2 : 3;
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        firstScmAccounts: _.first(scmAccounts, scmAccountsLimit),
        moreScmAccountsCount: scmAccounts.length - scmAccountsLimit
      });
    }
  });

});
