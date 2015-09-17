import _ from 'underscore';
import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import ChangePasswordView from './change-password-view';
import DeactivateView from './deactivate-view';
import GroupsView from './groups-view';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Templates['users-list-item'],

  events: {
    'click .js-user-more-scm': 'onMoreScmClick',
    'click .js-user-more-groups': 'onMoreGroupsClick',
    'click .js-user-update': 'onUpdateClick',
    'click .js-user-change-password': 'onChangePasswordClick',
    'click .js-user-deactivate': 'onDeactivateClick',
    'click .js-user-groups': 'onGroupsClick'
  },

  initialize: function () {
    this.scmLimit = 3;
    this.groupsLimit = 3;
  },

  onRender: function () {
    this.$el.attr('data-login', this.model.id);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onMoreScmClick: function (e) {
    e.preventDefault();
    this.showMoreScm();
  },

  onMoreGroupsClick: function (e) {
    e.preventDefault();
    this.showMoreGroups();
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

  onGroupsClick: function (e) {
    e.preventDefault();
    this.showGroups();
  },

  showMoreScm: function () {
    this.scmLimit = 10000;
    this.render();
  },

  showMoreGroups: function () {
    this.groupsLimit = 10000;
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

  showGroups: function () {
    new GroupsView({ model: this.model }).render();
  },

  serializeData: function () {
    var scmAccounts = this.model.get('scmAccounts'),
        scmAccountsLimit = scmAccounts.length > this.scmLimit ? this.scmLimit - 1 : this.scmLimit,
        groups = this.model.get('groups'),
        groupsLimit = groups.length > this.groupsLimit ? this.groupsLimit - 1 : this.groupsLimit;
    return _.extend(this._super(), {
      firstScmAccounts: _.first(scmAccounts, scmAccountsLimit),
      moreScmAccountsCount: scmAccounts.length - scmAccountsLimit,
      firstGroups: _.first(groups, groupsLimit),
      moreGroupsCount: groups.length - groupsLimit
    });
  }
});


