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

  defaults: {
    period: 0
  },

  url: function () {
    return baseUrl + '/api/qualitygates';
  },

  createUrl: function () {
    return this.url() + '/create_condition';
  },

  updateUrl: function () {
    return this.url() + '/update_condition';
  },

  deleteUrl: function () {
    return this.url() + '/delete_condition';
  },

  sync: function (method, model, options) {
    var opts = options || {};
    opts.type = 'POST';
    if (method === 'create') {
      opts.url = this.createUrl();
      opts.data = model.toJSON();
    }
    if (method === 'update') {
      opts.url = this.updateUrl();
      opts.data = model.toJSON();
    }
    if (method === 'delete') {
      opts.url = this.deleteUrl();
      opts.data = { id: model.id };
    }
    if (opts.data.period === '0') {
      delete opts.data.period;
    }
    return Backbone.ajax(opts);
  }
});


