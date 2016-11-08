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
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Controller from '../../components/navigator/controller';
import ComponentViewer from './component-viewer/main';

const FACET_DATA_FIELDS = ['components', 'users', 'rules', 'languages'];

export default Controller.extend({
  _facetsFromServer () {
    const facets = Controller.prototype._facetsFromServer.apply(this, arguments) || [];
    if (facets.indexOf('assignees') !== -1) {
      facets.push('assigned_to_me');
    }
    return facets;
  },

  _issuesParameters () {
    return {
      p: this.options.app.state.get('page'),
      ps: this.pageSize,
      s: 'FILE_LINE',
      asc: true,
      additionalFields: '_all',
      facets: this._facetsFromServer().join()
    };
  },

  _myIssuesFromResponse (r) {
    const myIssuesData = _.findWhere(r.facets, { property: 'assigned_to_me' });
    if ((myIssuesData != null) && _.isArray(myIssuesData.values) && myIssuesData.values.length > 0) {
      return this.options.app.state.set({ myIssues: myIssuesData.values[0].count }, { silent: true });
    } else {
      return this.options.app.state.unset('myIssues', { silent: true });
    }
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
    _.extend(data, this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(window.baseUrl + '/api/issues/search', data).done(function (r) {
      const issues = that.options.app.list.parseIssues(r);
      if (firstPage) {
        that.options.app.list.reset(issues);
      } else {
        that.options.app.list.add(issues);
      }
      that.options.app.list.setIndex();
      FACET_DATA_FIELDS.forEach(function (field) {
        that.options.app.facets[field] = r[field];
      });
      that.options.app.facets.reset(that._allFacets());
      that.options.app.facets.add(_.reject(r.facets, function (f) {
        return f.property === 'assigned_to_me';
      }), { merge: true });
      that._myIssuesFromResponse(r);
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
        return that.showComponentViewer(that.options.app.list.first());
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
    if (id === 'assignees') {
      return this.requestAssigneeFacet();
    }
    const facet = this.options.app.facets.get(id);
    const data = _.extend({ facets: id, ps: 1, additionalFields: '_all' }, this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(window.baseUrl + '/api/issues/search', data, function (r) {
      FACET_DATA_FIELDS.forEach(function (field) {
        that.options.app.facets[field] = that._mergeCollections(that.options.app.facets[field], r[field]);
      });
      const facetData = _.findWhere(r.facets, { property: id });
      if (facetData != null) {
        return facet.set(facetData);
      }
    });
  },

  requestAssigneeFacet () {
    const that = this;
    const facet = this.options.app.facets.get('assignees');
    const data = _.extend({ facets: 'assignees,assigned_to_me', ps: 1, additionalFields: '_all' },
        this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(window.baseUrl + '/api/issues/search', data, function (r) {
      FACET_DATA_FIELDS.forEach(function (field) {
        that.options.app.facets[field] = that._mergeCollections(that.options.app.facets[field], r[field]);
      });
      const facetData = _.findWhere(r.facets, { property: 'assignees' });
      that._myIssuesFromResponse(r);
      if (facetData != null) {
        return facet.set(facetData);
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

  getQuery (separator, addContext) {
    if (separator == null) {
      separator = '|';
    }
    if (addContext == null) {
      addContext = false;
    }
    const filter = this.options.app.state.get('query');
    if (addContext && this.options.app.state.get('isContext')) {
      _.extend(filter, this.options.app.state.get('contextQuery'));
    }
    const route = [];
    _.map(filter, function (value, property) {
      return route.push(`${property}=${encodeURIComponent(value)}`);
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
      projectName: issue.get('projectLongName')
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

