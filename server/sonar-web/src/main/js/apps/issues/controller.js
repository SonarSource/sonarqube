/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import $ from 'jquery';
import Backbone from 'backbone';
import Controller from '../../components/navigator/controller';
import ComponentViewer from './component-viewer/main';

const FACET_DATA_FIELDS = ['components', 'users', 'rules', 'languages'];

export default Controller.extend({
  _issuesParameters () {
    return {
      p: this.options.app.state.get('page'),
      ps: this.pageSize,
      asc: true,
      additionalFields: '_all',
      facets: this._facetsFromServer().join()
    };
  },

  fetchList (firstPage) {
    const that = this;
    if (firstPage == null) {
      firstPage = true;
    }
    if (firstPage) {
      this.options.app.state.set({ selectedIndex: 0, page: 1 }, { silent: true });
      this.closeComponentViewer();
    }
    const data = this._issuesParameters();
    Object.assign(data, this.options.app.state.get('query'));
    if (this.options.app.state.get('query').assigned_to_me) {
      Object.assign(data, { assignees: '__me__' });
    }
    if (this.options.app.state.get('isContext')) {
      Object.assign(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(window.baseUrl + '/api/issues/search', data).done(r => {
      const issues = that.options.app.list.parseIssues(r);
      if (firstPage) {
        that.options.app.list.reset(issues);
      } else {
        that.options.app.list.add(issues);
      }
      that.options.app.list.setIndex();
      FACET_DATA_FIELDS.forEach(field => {
        that.options.app.facets[field] = r[field];
      });
      that.options.app.facets.reset(that._allFacets());
      that.options.app.facets.add(r.facets, { merge: true });
      that.enableFacets(that._enabledFacets());
      if (firstPage) {
        that.options.app.state.set({
          page: r.p,
          pageSize: r.ps,
          total: r.total,
          maxResultsReached: r.p * r.ps >= r.total
        });
      } else {
        that.options.app.state.set({
          page: r.p,
          maxResultsReached: r.p * r.ps >= r.total
        });
      }
      if (firstPage && that.isIssuePermalink()) {
        that.showComponentViewer(that.options.app.list.first());
      }
    });
  },

  isIssuePermalink () {
    const query = this.options.app.state.get('query');
    return (query.issues != null) && this.options.app.list.length === 1;
  },

  _mergeCollections (a, b) {
    const collection = new Backbone.Collection(a);
    collection.add(b, { merge: true });
    return collection.toJSON();
  },

  requestFacet (id) {
    const that = this;
    const facet = this.options.app.facets.get(id);
    const data = { facets: id, ps: 1, additionalFields: '_all', ...this.options.app.state.get('query') };
    if (this.options.app.state.get('query').assigned_to_me) {
      Object.assign(data, { assignees: '__me__' });
    }
    if (this.options.app.state.get('isContext')) {
      Object.assign(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(window.baseUrl + '/api/issues/search', data, r => {
      FACET_DATA_FIELDS.forEach(field => {
        that.options.app.facets[field] = that._mergeCollections(that.options.app.facets[field], r[field]);
      });
      const facetData = r.facets.find(facet => facet.property === id);
      if (facetData != null) {
        facet.set(facetData);
      }
    });
  },

  newSearch () {
    this.options.app.state.unset('filter');
    return this.options.app.state.setQuery({ resolved: 'false' });
  },

  parseQuery () {
    const q = Controller.prototype.parseQuery.apply(this, arguments);
    delete q.asc;
    delete q.s;
    delete q.id;
    return q;
  },

  getQueryAsObject () {
    const state = this.options.app.state;
    const query = state.get('query');
    if (query.assigned_to_me) {
      Object.assign(query, { assignees: '__me__' });
    }
    if (state.get('isContext')) {
      Object.assign(query, state.get('contextQuery'));
    }
    return query;
  },

  getQuery (separator, addContext, handleMyIssues = false) {
    if (separator == null) {
      separator = '|';
    }
    if (addContext == null) {
      addContext = false;
    }
    const filter = this.options.app.state.get('query');
    if (addContext && this.options.app.state.get('isContext')) {
      Object.assign(filter, this.options.app.state.get('contextQuery'));
    }
    if (handleMyIssues && this.options.app.state.get('query').assigned_to_me) {
      Object.assign(filter, { assignees: '__me__' });
    }
    const route = [];
    Object.keys(filter).forEach(property => {
      route.push(`${property}=${encodeURIComponent(filter[property])}`);
    });
    return route.join(separator);
  },

  _prepareComponent (issue) {
    return {
      key: issue.get('component'),
      name: issue.get('componentLongName'),
      qualifier: issue.get('componentQualifier'),
      subProject: issue.get('subProject'),
      subProjectName: issue.get('subProjectLongName'),
      project: issue.get('project'),
      projectName: issue.get('projectLongName'),
      projectOrganization: issue.get('projectOrganization')
    };
  },

  showComponentViewer (issue) {
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    key.setScope('componentViewer');
    this.options.app.issuesView.unbindScrollEvents();
    this.options.app.state.set('component', this._prepareComponent(issue));
    this.options.app.componentViewer = new ComponentViewer({ app: this.options.app });
    this.options.app.layout.workspaceComponentViewerRegion.show(this.options.app.componentViewer);
    this.options.app.layout.showComponentViewer();
    return this.options.app.componentViewer.openFileByIssue(issue);
  },

  closeComponentViewer () {
    key.setScope('list');
    $('body').click();
    this.options.app.state.unset('component');
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    this.options.app.layout.hideComponentViewer();
    this.options.app.issuesView.bindScrollEvents();
    return this.options.app.issuesView.scrollTo();
  }
});

