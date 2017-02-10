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
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults: {
    _hidden: false,
    _system: false
  },

  _matchAttribute (attr, query) {
    const value = this.get(attr) || '';
    return value.toLowerCase().includes(query.toLowerCase());
  },

  match (query) {
    return this._matchAttribute('name', query) ||
        this._matchAttribute('category', query) ||
        this._matchAttribute('description', query);
  },

  _action (options) {
    const that = this;
    const opts = {
      ...options,
      type: 'POST',
      data: { key: this.id },
      success () {
        options.success(that);
      },
      error (jqXHR) {
        that.set({ _status: 'failed', _errors: jqXHR.responseJSON.errors });
      }
    };
    const xhr = Backbone.ajax(opts);
    this.trigger('request', this, xhr);
    return xhr;
  },

  install () {
    return this._action({
      url: window.baseUrl + '/api/plugins/install',
      success (model) {
        model.set({ _status: 'installing' });
      }
    });
  },

  update () {
    return this._action({
      url: window.baseUrl + '/api/plugins/update',
      success (model) {
        model.set({ _status: 'updating' });
      }
    });
  },

  uninstall () {
    return this._action({
      url: window.baseUrl + '/api/plugins/uninstall',
      success (model) {
        model.set({ _status: 'uninstalling' });
      }
    });
  }
});

