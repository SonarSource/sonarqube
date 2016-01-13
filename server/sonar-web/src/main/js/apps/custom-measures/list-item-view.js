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
import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import DeleteView from './delete-view';
import Template from './templates/custom-measures-list-item.hbs';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  events: {
    'click .js-custom-measure-update': 'onUpdateClick',
    'click .js-custom-measure-delete': 'onDeleteClick'
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
    this.updateCustomMeasure();
  },

  onDeleteClick: function (e) {
    e.preventDefault();
    this.deleteCustomMeasure();
  },

  updateCustomMeasure: function () {
    new UpdateView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteCustomMeasure: function () {
    new DeleteView({ model: this.model }).render();
  }
});


