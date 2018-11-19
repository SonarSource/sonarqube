/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import ChangePasswordView from './change-password-view';
import DeactivateView from './deactivate-view';
import GroupsView from './groups-view';
import TokensView from './tokens-view';
import Template from './templates/users-list-item.hbs';
import { areThereCustomOrganizations } from '../../store/organizations/utils';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  events: {
    'click .js-user-more-scm': 'onMoreScmClick',
    'click .js-user-more-groups': 'onMoreGroupsClick',
    'click .js-user-update': 'onUpdateClick',
    'click .js-user-change-password': 'onChangePasswordClick',
    'click .js-user-deactivate': 'onDeactivateClick',
    'click .js-user-groups': 'onGroupsClick',
    'click .js-user-tokens': 'onTokensClick'
  },

  initialize() {
    this.scmLimit = 3;
    this.groupsLimit = 3;
  },

  onRender() {
    this.$el.attr('data-login', this.model.id);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onMoreScmClick(e) {
    e.preventDefault();
    this.showMoreScm();
  },

  onMoreGroupsClick(e) {
    e.preventDefault();
    this.showMoreGroups();
  },

  onUpdateClick(e) {
    e.preventDefault();
    this.updateUser();
  },

  onChangePasswordClick(e) {
    e.preventDefault();
    this.changePassword();
  },

  onDeactivateClick(e) {
    e.preventDefault();
    this.deactivateUser();
  },

  onGroupsClick(e) {
    e.preventDefault();
    this.showGroups();
  },

  onTokensClick(e) {
    e.preventDefault();
    this.showTokens();
  },

  showMoreScm() {
    this.scmLimit = 10000;
    this.render();
  },

  showMoreGroups() {
    this.groupsLimit = 10000;
    this.render();
  },

  updateUser() {
    new UpdateView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  changePassword() {
    new ChangePasswordView({
      model: this.model,
      collection: this.model.collection,
      currentUser: this.options.currentUser
    }).render();
  },

  deactivateUser() {
    new DeactivateView({ model: this.model }).render();
  },

  showGroups() {
    new GroupsView({ model: this.model }).render();
  },

  showTokens() {
    new TokensView({ model: this.model }).render();
  },

  serializeData() {
    const scmAccounts = this.model.get('scmAccounts');
    const scmAccountsLimit = scmAccounts.length > this.scmLimit ? this.scmLimit - 1 : this.scmLimit;

    const groups = this.model.get('groups');
    const groupsLimit = groups.length > this.groupsLimit ? this.groupsLimit - 1 : this.groupsLimit;

    const externalProvider = this.model.get('externalProvider');
    const identityProvider = this.model.get('local')
      ? null
      : this.options.providers.find(provider => externalProvider === provider.key);

    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      identityProvider,
      firstScmAccounts: scmAccounts.slice(0, scmAccountsLimit),
      moreScmAccountsCount: scmAccounts.length - scmAccountsLimit,
      firstGroups: groups.slice(0, groupsLimit),
      moreGroupsCount: groups.length - groupsLimit,
      customOrganizations: areThereCustomOrganizations()
    };
  }
});
