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
import $ from 'jquery';
import { findLastIndex } from 'lodash';
import Backbone from 'backbone';
import Plugin from './plugin';

const Plugins = Backbone.Collection.extend({
  model: Plugin,

  comparator(model) {
    return model.get('name') || '';
  },

  initialize() {
    this._installedCount = 0;
    this._updatedCount = 0;
    this._uninstalledCount = 0;
    this.listenTo(this, 'change:_status', this.onStatusChange);
  },

  parse(r) {
    const that = this;
    return r.plugins.map(plugin => {
      let updates = [
        that._getLastWithStatus(plugin.updates, 'COMPATIBLE'),
        that._getLastWithStatus(plugin.updates, 'REQUIRES_SYSTEM_UPGRADE'),
        that._getLastWithStatus(plugin.updates, 'DEPS_REQUIRE_SYSTEM_UPGRADE')
      ].filter(update => update);
      updates = updates.map(update => that._extendChangelog(plugin.updates, update));
      return { ...plugin, updates };
    });
  },

  _getLastWithStatus(updates, status) {
    const index = findLastIndex(updates, update => update.status === status);
    return index !== -1 ? updates[index] : null;
  },

  _extendChangelog(updates, update) {
    const index = updates.indexOf(update);
    const previousUpdates = index > 0 ? updates.slice(0, index) : [];
    return { ...update, previousUpdates };
  },

  _fetchInstalled() {
    if (this._installed) {
      return $.Deferred().resolve().promise();
    }
    const that = this;
    const opts = {
      type: 'GET',
      url: window.baseUrl + '/api/plugins/installed?f=category',
      success(r) {
        that._installed = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchUpdates() {
    if (this._updates) {
      return $.Deferred().resolve().promise();
    }
    const that = this;
    const opts = {
      type: 'GET',
      url: window.baseUrl + '/api/plugins/updates',
      success(r) {
        that._updates = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchAvailable() {
    if (this._available) {
      return $.Deferred().resolve().promise();
    }
    const that = this;
    const opts = {
      type: 'GET',
      url: window.baseUrl + '/api/plugins/available',
      success(r) {
        that._available = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchPending() {
    const that = this;
    const opts = {
      type: 'GET',
      url: window.baseUrl + '/api/plugins/pending',
      success(r) {
        const installing = r.installing.map(plugin => {
          return { key: plugin.key, _status: 'installing' };
        });
        const updating = r.updating.map(plugin => {
          return { key: plugin.key, _status: 'updating' };
        });
        const uninstalling = r.removing.map(plugin => {
          return { key: plugin.key, _status: 'uninstalling' };
        });
        that._installedCount = installing.length;
        that._updatedCount = updating.length;
        that._uninstalledCount = uninstalling.length;
        that._pending = new Plugins([].concat(installing, updating, uninstalling)).models;
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchSystemUpgrades() {
    if (this._systemUpdates) {
      return $.Deferred().resolve().promise();
    }
    const that = this;
    const opts = {
      type: 'GET',
      url: window.baseUrl + '/api/system/upgrades',
      success(r) {
        that._systemUpdates = r.upgrades.map(update => ({ ...update, _system: true }));
      }
    };
    return Backbone.ajax(opts);
  },

  fetchInstalled() {
    const that = this;
    return $.when(this._fetchInstalled(), this._fetchUpdates(), this._fetchPending()).done(() => {
      const plugins = new Plugins();
      plugins.set(that._installed);
      plugins.set(that._updates, { remove: false });
      plugins.set(that._pending, { add: false, remove: false });
      that.reset(plugins.models);
    });
  },

  fetchUpdates() {
    const that = this;
    return $.when(this._fetchInstalled(), this._fetchUpdates(), this._fetchPending()).done(() => {
      const plugins = new Plugins();
      plugins.set(that._installed);
      plugins.set(that._updates, { remove: true });
      plugins.set(that._pending, { add: false, remove: false });
      that.reset(plugins.models);
    });
  },

  fetchAvailable() {
    const that = this;
    return $.when(this._fetchAvailable(), this._fetchPending()).done(() => {
      const plugins = new Plugins();
      plugins.set(that._available);
      plugins.set(that._pending, { add: false, remove: false });
      that.reset(plugins.models);
    });
  },

  fetchSystemUpgrades() {
    const that = this;
    return $.when(this._fetchSystemUpgrades()).done(() => {
      that.reset(that._systemUpdates);
    });
  },

  search(query) {
    /* eslint-disable array-callback-return */
    this.filter(model => {
      model.set({ _hidden: !model.match(query) });
    });
  },

  cancelAll() {
    const that = this;
    const opts = {
      type: 'POST',
      url: window.baseUrl + '/api/plugins/cancel_all',
      success() {
        that._installedCount = 0;
        that._updatedCount = 0;
        that._uninstalledCount = 0;
        that.forEach(model => {
          model.unset('_status');
        });
        that.trigger('change');
      }
    };
    return Backbone.ajax(opts);
  },

  onStatusChange(model, status) {
    if (status === 'installing') {
      this._installedCount++;
    }
    if (status === 'updating') {
      this._updatedCount++;
    }
    if (status === 'uninstalling') {
      this._uninstalledCount++;
    }
    this.trigger('change');
  }
});

export default Plugins;
