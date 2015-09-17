import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  defaults: {
    _hidden: false,
    _system: false
  },

  _matchAttribute: function (attr, query) {
    var value = this.get(attr) || '';
    return value.search(new RegExp(query, 'i')) !== -1;
  },

  match: function (query) {
    return this._matchAttribute('name', query) ||
        this._matchAttribute('category', query) ||
        this._matchAttribute('description', query);
  },

  _action: function (options) {
    var that = this;
    var opts = _.extend({}, options, {
      type: 'POST',
      data: { key: this.id },
      beforeSend: function () {
        // disable global ajax notifications
      },
      success: function () {
        options.success(that);
      },
      error: function (jqXHR) {
        that.set({ _status: 'failed', _errors: jqXHR.responseJSON.errors });
      }
    });
    var xhr = Backbone.ajax(opts);
    this.trigger('request', this, xhr);
    return xhr;
  },

  install: function () {
    return this._action({
      url: baseUrl + '/api/plugins/install',
      success: function (model) {
        model.set({ _status: 'installing' });
      }
    });
  },

  update: function () {
    return this._action({
      url: baseUrl + '/api/plugins/update',
      success: function (model) {
        model.set({ _status: 'installing' });
      }
    });
  },

  uninstall: function () {
    return this._action({
      url: baseUrl + '/api/plugins/uninstall',
      success: function (model) {
        model.set({ _status: 'uninstalling' });
      }
    });
  }
});


