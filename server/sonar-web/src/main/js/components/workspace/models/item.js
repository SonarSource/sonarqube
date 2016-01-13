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
import Backbone from 'backbone';

export default Backbone.Model.extend({

  validate: function () {
    if (!this.has('type')) {
      return 'type is missing';
    }
    if (this.get('type') === 'component' && !this.has('uuid')) {
      return 'uuid is missing';
    }
    if (this.get('type') === 'rule' && !this.has('key')) {
      return 'key is missing';
    }
  },

  isComponent: function () {
    return this.get('type') === 'component';
  },

  isRule: function () {
    return this.get('type') === 'rule';
  },

  destroy: function (options) {
    this.stopListening();
    this.trigger('destroy', this, this.collection, options);
  }
});


