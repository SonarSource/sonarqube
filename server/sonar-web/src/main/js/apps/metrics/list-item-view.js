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
import DeleteView from './delete-view';
import Template from './templates/metrics-list-item.hbs';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Template,

  events: {
    'click .js-metric-update': 'onUpdateClick',
    'click .js-metric-delete': 'onDeleteClick'
  },

  onRender() {
    this.$el.attr('data-id', this.model.id).attr('data-key', this.model.get('key'));
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onUpdateClick(e) {
    e.preventDefault();
    this.updateMetric();
  },

  onDeleteClick(e) {
    e.preventDefault();
    this.deleteMetric();
  },

  updateMetric() {
    new UpdateView({
      model: this.model,
      collection: this.model.collection,
      types: this.options.types,
      domains: this.options.domains
    }).render();
  },

  deleteMetric() {
    new DeleteView({ model: this.model }).render();
  }
});
