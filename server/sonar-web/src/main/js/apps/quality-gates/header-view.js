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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import RenameView from './rename-view';
import CopyView from './copy-view';
import DeleteView from './delete-view';
import Template from './templates/quality-gate-detail-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click #quality-gate-rename': 'renameQualityGate',
    'click #quality-gate-copy': 'copyQualityGate',
    'click #quality-gate-delete': 'deleteQualityGate',
    'click #quality-gate-toggle-default': 'toggleDefault'
  },

  renameQualityGate: function () {
    new RenameView({
      model: this.model
    }).render();
  },

  copyQualityGate: function () {
    new CopyView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteQualityGate: function () {
    new DeleteView({
      model: this.model
    }).render();
  },

  toggleDefault: function () {
    this.model.toggleDefault();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit
    });
  }
});


