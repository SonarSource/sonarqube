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
      'click .js-user-more-scm': 'onMoreScmClick',
      'click .js-user-update': 'onUpdateClick',
      'click .js-user-change-password': 'onChangePasswordClick',
      'click .js-user-deactivate': 'onDeactivateClick'
    },

    initialize: function () {
      this.scmLimit = 3;
    },

    onRender: function () {
      this.$el.attr('data-login', this.model.id);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onMoreScmClick: function (e) {
      e.preventDefault();
      this.showMoreScm();
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

    showMoreScm: function () {
      this.scmLimit = 10000;
      this.render();
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
          scmAccountsLimit = scmAccounts.length > this.scmLimit ? this.scmLimit - 1 : this.scmLimit;
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        firstScmAccounts: _.first(scmAccounts, scmAccountsLimit),
        moreScmAccountsCount: scmAccounts.length - scmAccountsLimit
      });
    }
  });

});
