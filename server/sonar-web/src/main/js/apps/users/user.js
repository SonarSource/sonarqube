import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'login',

  urlRoot: function () {
    return baseUrl + '/api/users';
  },

  defaults: function () {
    return {
      groups: [],
      scmAccounts: []
    };
  },

  toQuery: function () {
    var q = this.toJSON();
    _.each(q, function (value, key) {
      if (_.isArray(value)) {
        q[key] = value.join(',');
      }
    });
    return q;
  },

  isNew: function () {
    // server never sends a password
    return this.has('password');
  },

  sync: function (method, model, options) {
    var opts = options || {};
    if (method === 'create') {
      _.defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data: _.pick(model.toQuery(), 'login', 'name', 'email', 'password', 'scmAccounts')
      });
    }
    if (method === 'update') {
      _.defaults(opts, {
        url: this.urlRoot() + '/update',
        type: 'POST',
        data: _.pick(model.toQuery(), 'login', 'name', 'email', 'scmAccounts')
      });
    }
    if (method === 'delete') {
      _.defaults(opts, {
        url: this.urlRoot() + '/deactivate',
        type: 'POST',
        data: { login: this.id }
      });
    }
    return Backbone.ajax(opts);
  },

  changePassword: function (password, options) {
    var opts = _.defaults(options || {}, {
      url: this.urlRoot() + '/change_password',
      type: 'POST',
      data: {
        login: this.id,
        password: password
      }
    });
    return Backbone.ajax(opts);
  }
});


