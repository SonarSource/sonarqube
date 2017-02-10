/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Template from './templates/update-center-header.hbs';
import RestartModal from '../../components/RestartModal';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    all: 'render'
  },

  events: {
    'click .js-restart': 'restart',
    'click .js-cancel-all': 'cancelAll'
  },

  restart () {
    new RestartModal().render();
  },

  cancelAll () {
    this.collection.cancelAll();
  },

  serializeData () {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      installing: this.collection._installedCount,
      updating: this.collection._updatedCount,
      uninstalling: this.collection._uninstalledCount
    };
  }
});

