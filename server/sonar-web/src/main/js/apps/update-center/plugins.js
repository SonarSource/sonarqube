import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Plugin from './plugin';

var Plugins = Backbone.Collection.extend({
  model: Plugin,

  comparator: function (model) {
    return model.get('name') || '';
  },

  initialize: function () {
    this._installedCount = 0;
    this._uninstalledCount = 0;
    this.listenTo(this, 'change:_status', this.onStatusChange);
  },

  parse: function (r) {
    var that = this;
    return r.plugins.map(function (plugin) {
      var updates = [
        that._getLastWithStatus(plugin.updates, 'COMPATIBLE'),
        that._getLastWithStatus(plugin.updates, 'REQUIRES_SYSTEM_UPGRADE'),
        that._getLastWithStatus(plugin.updates, 'DEPS_REQUIRE_SYSTEM_UPGRADE')
      ].filter(_.identity);
      updates = updates.map(function (update) {
        return that._extendChangelog(plugin.updates, update);
      });
      return _.extend(plugin, { updates: updates });
    });
  },

  _getLastWithStatus: function (updates, status) {
    var index = _.findLastIndex(updates, function (update) {
      return update.status === status;
    });
    return index !== -1 ? updates[index] : null;
  },

  _extendChangelog: function (updates, update) {
    var index = updates.indexOf(update);
    var previousUpdates = index > 0 ? updates.slice(0, index) : [];
    return _.extend(update, { previousUpdates: previousUpdates });
  },

  _fetchInstalled: function () {
    if (this._installed) {
      return $.Deferred().resolve().promise();
    }
    var that = this;
    var opts = {
      type: 'GET',
      url: baseUrl + '/api/plugins/installed',
      success: function (r) {
        that._installed = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchUpdates: function () {
    if (this._updates) {
      return $.Deferred().resolve().promise();
    }
    var that = this;
    var opts = {
      type: 'GET',
      url: baseUrl + '/api/plugins/updates',
      success: function (r) {
        that._updates = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchAvailable: function () {
    if (this._available) {
      return $.Deferred().resolve().promise();
    }
    var that = this;
    var opts = {
      type: 'GET',
      url: baseUrl + '/api/plugins/available',
      success: function (r) {
        that._available = that.parse(r);
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchPending: function () {
    var that = this;
    var opts = {
      type: 'GET',
      url: baseUrl + '/api/plugins/pending',
      success: function (r) {
        var installing = r.installing.map(function (plugin) {
              return { key: plugin.key, _status: 'installing' };
            }),
            uninstalling = r.removing.map(function (plugin) {
              return { key: plugin.key, _status: 'uninstalling' };
            });
        that._installedCount = installing.length;
        that._uninstalledCount = uninstalling.length;
        that._pending = new Plugins([].concat(installing, uninstalling)).models;
      }
    };
    return Backbone.ajax(opts);
  },

  _fetchSystemUpgrades: function () {
    if (this._systemUpdates) {
      return $.Deferred().resolve().promise();
    }
    var that = this;
    var opts = {
      type: 'GET',
      url: baseUrl + '/api/system/upgrades',
      success: function (r) {
        that._systemUpdates = r.upgrades.map(function (update) {
          return _.extend(update, { _system: true });
        });
      }
    };
    return Backbone.ajax(opts);
  },

  fetchInstalled: function () {
    var that = this;
    return $.when(this._fetchInstalled(), this._fetchUpdates(), this._fetchPending()).done(function () {
      var plugins = new Plugins();
      plugins.set(that._installed);
      plugins.set(that._updates, { remove: false });
      plugins.set(that._pending, { add: false, remove: false });
      that.reset(plugins.models);
    });
  },

  fetchUpdates: function () {
    var that = this;
    return $.when(this._fetchInstalled(), this._fetchUpdates(), this._fetchPending())
        .done(function () {
          var plugins = new Plugins();
          plugins.set(that._installed);
          plugins.set(that._updates, { remove: true });
          plugins.set(that._pending, { add: false, remove: false });
          that.reset(plugins.models);
        });
  },

  fetchAvailable: function () {
    var that = this;
    return $.when(this._fetchAvailable(), this._fetchPending()).done(function () {
      var plugins = new Plugins();
      plugins.set(that._available);
      plugins.set(that._pending, { add: false, remove: false });
      that.reset(plugins.models);
    });
  },

  fetchSystemUpgrades: function () {
    var that = this;
    return $.when(this._fetchSystemUpgrades()).done(function () {
      that.reset(that._systemUpdates);
    });
  },

  search: function (query) {
    this.filter(function (model) {
      model.set({ _hidden: !model.match(query) });
    });
  },

  cancelAll: function () {
    var that = this;
    var opts = {
      type: 'POST',
      url: baseUrl + '/api/plugins/cancel_all',
      success: function () {
        that._installedCount = 0;
        that._uninstalledCount = 0;
        that.forEach(function (model) {
          model.unset('_status');
        });
        that.trigger('change');
      }
    };
    return Backbone.ajax(opts);
  },

  onStatusChange: function (model, status) {
    if (status === 'installing') {
      this._installedCount++;
    }
    if (status === 'uninstalling') {
      this._uninstalledCount++;
    }
    this.trigger('change');
  }
});

export default Plugins;


