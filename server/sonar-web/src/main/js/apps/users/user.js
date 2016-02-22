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
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'login',

  urlRoot () {
    return '/api/users';
  },

  defaults () {
    return {
      groups: [],
      scmAccounts: []
    };
  },

  toQuery () {
    const q = this.toJSON();
    _.each(q, function (value, key) {
      if (_.isArray(value)) {
        q[key] = value.join(',');
      }
    });
    return q;
  },

  isNew () {
    // server never sends a password
    return this.has('password');
  },

  sync (method, model, options) {
    const opts = options || {};
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

  changePassword (oldPassword, password, options) {
    const data = {
      login: this.id,
      password
    };
    if (oldPassword != null) {
      data.previousPassword = oldPassword;
    }
    const opts = _.defaults(options || {}, {
      url: this.urlRoot() + '/change_password',
      type: 'POST',
      data
    });
    return Backbone.ajax(opts);
  }
});


