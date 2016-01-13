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
import Router from '../../components/navigator/router';

export default Router.extend({
  routes: {
    '': 'home',
    ':query': 'index'
  },

  initialize: function (options) {
    Router.prototype.initialize.apply(this, arguments);
    this.listenTo(options.app.state, 'change:filter', this.updateRoute);
  },

  home: function () {
    if (this.options.app.state.get('isContext')) {
      return this.navigate('resolved=false', { trigger: true, replace: true });
    } else {
      return this.options.app.controller.showHomePage();
    }
  },

  index: function (query) {
    var that = this;
    query = this.options.app.controller.parseQuery(query);
    if (query.id != null) {
      var filter = this.options.app.filters.get(query.id);
      delete query.id;
      if (Object.keys(query).length > 0) {
        that.options.app.controller.applyFilter(filter, true);
        that.options.app.state.setQuery(query);
        that.options.app.state.set({ changed: true });
      } else {
        that.options.app.controller.applyFilter(filter);
      }
    } else {
      this.options.app.state.setQuery(query);
    }
  }
});


