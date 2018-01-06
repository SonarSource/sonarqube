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
import Backbone from 'backbone';

export default Backbone.Model.extend({
  defaults() {
    return {
      page: 1,
      maxResultsReached: false,
      query: {},
      facets: []
    };
  },

  nextPage() {
    const page = this.get('page');
    this.set({ page: page + 1 });
  },

  clearQuery(query) {
    const q = {};
    Object.keys(query).forEach(key => {
      if (query[key]) {
        q[key] = query[key];
      }
    });
    return q;
  },

  _areQueriesEqual(a, b) {
    let equal = Object.keys(a).length === Object.keys(b).length;
    Object.keys(a).forEach(key => {
      equal = equal && a[key] === b[key];
    });
    return equal;
  },

  updateFilter(obj, options) {
    const oldQuery = this.get('query');
    let query = { ...oldQuery, ...obj };
    const opts = { force: false, ...options };
    query = this.clearQuery(query);
    if (opts.force || !this._areQueriesEqual(oldQuery, query)) {
      this.setQuery(query);
    }
  },

  setQuery(query) {
    this.set({ query }, { silent: true });
    this.set({ changed: true });
    this.trigger('change:query');
  }
});
