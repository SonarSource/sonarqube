/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
import $ from 'jquery';
import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import DeleteView from './delete-view';
import UsersView from './users-view';
import Template from './templates/groups-list-item.hbs';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Template,

  events: {
    'click .js-group-update': 'onUpdateClick',
    'click .js-group-delete': 'onDeleteClick',
    'click .js-group-users': 'onUsersClick'
  },

  onRender: function () {
    this.$el.attr('data-id', this.model.id);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onUpdateClick: function (e) {
    e.preventDefault();
    this.updateGroup();
  },

  onDeleteClick: function (e) {
    e.preventDefault();
    this.deleteGroup();
  },

  onUsersClick: function (e) {
    e.preventDefault();
    $('.tooltip').remove();
    this.showUsers();
  },

  updateGroup: function () {
    new UpdateView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteGroup: function () {
    new DeleteView({ model: this.model }).render();
  },

  showUsers: function () {
    new UsersView({ model: this.model }).render();
  }
});


