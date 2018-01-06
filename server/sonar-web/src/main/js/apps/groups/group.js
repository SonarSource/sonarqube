/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import { defaults, pick } from 'lodash';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  urlRoot() {
    return window.baseUrl + '/api/user_groups';
  },

  sync(method, model, options) {
    const opts = options || {};
    if (method === 'create') {
      const data = pick(model.toJSON(), 'name', 'description');
      if (options.organization) {
        Object.assign(data, { organization: options.organization.key });
      }
      defaults(opts, {
        url: this.urlRoot() + '/create',
        type: 'POST',
        data
      });
    }
    if (method === 'update') {
      const data = {
        ...pick(model.changed, 'name', 'description'),
        id: model.id
      };
      defaults(opts, {
        url: this.urlRoot() + '/update',
        type: 'POST',
        data
      });
    }
    if (method === 'delete') {
      const data = { id: this.id };
      defaults(opts, {
        url: this.urlRoot() + '/delete',
        type: 'POST',
        data
      });
    }
    return Backbone.ajax(opts);
  }
});
