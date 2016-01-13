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

export default Backbone.Collection.extend({
  url: baseUrl + '/api/webservices/list',
  comparator: 'path',

  parse: function (r) {
    return r.webServices.map(function (webService) {
      var internal = _.every(webService.actions, function (action) {
            return action.internal;
          }),
          actions = webService.actions.map(function (action) {
            return _.extend(action, { path: webService.path });
          });
      return _.extend(webService, {
        internal: internal,
        actions: actions
      });
    });
  }
});
