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
import { uniq } from 'lodash';
import Marionette from 'backbone.marionette';

export default Marionette.Controller.extend({
  pageSize: 50,

  initialize(options) {
    this.app = options.app;
    this.listenTo(options.app.state, 'change:query', this.fetchList);
  },

  _allFacets() {
    return this.options.app.state.get('allFacets').map(facet => {
      return { property: facet };
    });
  },

  _enabledFacets() {
    const that = this;
    let facets = this.options.app.state.get('facets');
    const criteria = Object.keys(this.options.app.state.get('query'));
    facets = facets.concat(criteria);
    facets = facets.map(facet => {
      return that.options.app.state.get('transform')[facet] != null
        ? that.options.app.state.get('transform')[facet]
        : facet;
    });
    facets = uniq(facets);
    return facets.filter(facet => that.options.app.state.get('allFacets').indexOf(facet) !== -1);
  },

  _facetsFromServer() {
    const that = this;
    const facets = this._enabledFacets();
    return facets.filter(
      facet => that.options.app.state.get('facetsFromServer').indexOf(facet) !== -1
    );
  },

  fetchList() {},

  fetchNextPage() {
    this.options.app.state.nextPage();
    return this.fetchList(false);
  },

  enableFacet(id) {
    const facet = this.options.app.facets.get(id);
    if (facet.has('values') || this.options.app.state.get('facetsFromServer').indexOf(id) === -1) {
      facet.set({ enabled: true });
    } else {
      this.requestFacet(id).then(() => {
        facet.set({ enabled: true });
      });
    }
  },

  disableFacet(id) {
    const facet = this.options.app.facets.get(id);
    facet.set({ enabled: false });
    this.options.app.facetsView.children.findByModel(facet).disable();
  },

  toggleFacet(id) {
    const facet = this.options.app.facets.get(id);
    if (facet.get('enabled')) {
      this.disableFacet(id);
    } else {
      this.enableFacet(id);
    }
  },

  enableFacets(facets) {
    facets.forEach(this.enableFacet, this);
  },

  newSearch() {
    this.options.app.state.setQuery({});
  },

  parseQuery(query, separator) {
    separator = separator || '|';
    const q = {};
    (query || '').split(separator).forEach(t => {
      const tokens = t.split('=');
      if (tokens[0] && tokens[1] != null) {
        q[tokens[0]] = decodeURIComponent(tokens[1]);
      }
    });
    return q;
  },

  getQuery(separator) {
    separator = separator || '|';
    const filter = this.options.app.state.get('query');
    const route = [];
    Object.keys(filter).forEach(property => {
      route.push(`${property}=${encodeURIComponent(filter[property])}`);
    });
    return route.join(separator);
  },

  getRoute(separator) {
    separator = separator || '|';
    return this.getQuery(separator);
  },

  selectNext() {
    const index = this.options.app.state.get('selectedIndex') + 1;
    if (index < this.options.app.list.length) {
      this.options.app.state.set({ selectedIndex: index });
    } else if (!this.options.app.state.get('maxResultsReached')) {
      const that = this;
      this.fetchNextPage().then(() => {
        that.options.app.state.set({ selectedIndex: index });
      });
    } else {
      this.options.app.list.trigger('limitReached');
    }
  },

  selectPrev() {
    const index = this.options.app.state.get('selectedIndex') - 1;
    if (index >= 0) {
      this.options.app.state.set({ selectedIndex: index });
    } else {
      this.options.app.list.trigger('limitReached');
    }
  }
});
