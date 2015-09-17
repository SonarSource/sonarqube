import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Controller from 'components/navigator/controller';
import ComponentViewer from './component-viewer/main';
import HomeView from './workspace-home-view';

var FIELDS = 'component,componentId,project,subProject,rule,status,resolution,author,reporter,assignee,debt,line,' +
        'message,severity,actionPlan,creationDate,updateDate,closeDate,tags,comments,attr,actions,transitions,' +
        'actionPlanName',
    FACET_DATA_FIELDS = ['components', 'users', 'rules', 'actionPlans', 'languages'];

export default Controller.extend({
  _facetsFromServer: function () {
    var facets = Controller.prototype._facetsFromServer.apply(this, arguments) || [];
    facets.push('assigned_to_me');
    return facets;
  },

  _issuesParameters: function () {
    return {
      p: this.options.app.state.get('page'),
      ps: this.pageSize,
      s: 'FILE_LINE',
      asc: true,
      f: FIELDS,
      additionalFields: '_all',
      facets: this._facetsFromServer().join()
    };
  },

  _myIssuesFromResponse: function (r) {
    var myIssuesData = _.findWhere(r.facets, { property: 'assigned_to_me' });
    if ((myIssuesData != null) && _.isArray(myIssuesData.values) && myIssuesData.values.length > 0) {
      return this.options.app.state.set({ myIssues: myIssuesData.values[0].count }, { silent: true });
    } else {
      return this.options.app.state.unset('myIssues', { silent: true });
    }
  },

  fetchList: function (firstPage) {
    var that = this;
    if (firstPage == null) {
      firstPage = true;
    }
    if (firstPage) {
      this.options.app.state.set({ selectedIndex: 0, page: 1 }, { silent: true });
      this.hideHomePage();
      this.closeComponentViewer();
    }
    var data = this._issuesParameters();
    _.extend(data, this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(baseUrl + '/api/issues/search', data).done(function (r) {
      var issues = that.options.app.list.parseIssues(r);
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
      that.options.app.state.set({
        page: r.p,
        pageSize: r.ps,
        total: r.total,
        maxResultsReached: r.p * r.ps >= r.total
      });
      if (firstPage && that.isIssuePermalink()) {
        return that.showComponentViewer(that.options.app.list.first());
      }
    });
  },

  isIssuePermalink: function () {
    var query = this.options.app.state.get('query');
    return (query.issues != null) && this.options.app.list.length === 1;
  },

  fetchFilters: function () {
    var that = this;
    return $.when(
        that.options.app.filters.fetch({ reset: true }),
        $.get(baseUrl + '/api/issue_filters/app', function (r) {
          that.options.app.state.set({
            canBulkChange: r.canBulkChange,
            canManageFilters: r.canManageFilters
          });
        }));
  },

  _mergeCollections: function (a, b) {
    var collection = new Backbone.Collection(a);
    collection.add(b, { merge: true });
    return collection.toJSON();
  },

  requestFacet: function (id) {
    var that = this;
    if (id === 'assignees') {
      return this.requestAssigneeFacet();
    }
    var facet = this.options.app.facets.get(id),
        data = _.extend({ facets: id, ps: 1, additionalFields: '_all' }, this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(baseUrl + '/api/issues/search', data, function (r) {
      FACET_DATA_FIELDS.forEach(function (field) {
        that.options.app.facets[field] = that._mergeCollections(that.options.app.facets[field], r[field]);
      });
      var facetData = _.findWhere(r.facets, { property: id });
      if (facetData != null) {
        return facet.set(facetData);
      }
    });
  },

  requestAssigneeFacet: function () {
    var that = this;
    var facet = this.options.app.facets.get('assignees'),
        data = _.extend({ facets: 'assignees,assigned_to_me', ps: 1, additionalFields: '_all' },
            this.options.app.state.get('query'));
    if (this.options.app.state.get('isContext')) {
      _.extend(data, this.options.app.state.get('contextQuery'));
    }
    return $.get(baseUrl + '/api/issues/search', data, function (r) {
      FACET_DATA_FIELDS.forEach(function (field) {
        that.options.app.facets[field] = that._mergeCollections(that.options.app.facets[field], r[field]);
      });
      var facetData = _.findWhere(r.facets, { property: 'assignees' });
      that._myIssuesFromResponse(r);
      if (facetData != null) {
        return facet.set(facetData);
      }
    });
  },

  newSearch: function () {
    this.options.app.state.unset('filter');
    return this.options.app.state.setQuery({ resolved: 'false' });
  },

  applyFilter: function (filter, ignoreQuery) {
    if (ignoreQuery == null) {
      ignoreQuery = false;
    }
    if (!ignoreQuery) {
      var filterQuery = this.parseQuery(filter.get('query'));
      this.options.app.state.setQuery(filterQuery);
    }
    return this.options.app.state.set({ filter: filter, changed: false });
  },

  parseQuery: function () {
    var q = Controller.prototype.parseQuery.apply(this, arguments);
    delete q.asc;
    delete q.s;
    return q;
  },

  getQuery: function (separator, addContext) {
    if (separator == null) {
      separator = '|';
    }
    if (addContext == null) {
      addContext = false;
    }
    var filter = this.options.app.state.get('query');
    if (addContext && this.options.app.state.get('isContext')) {
      _.extend(filter, this.options.app.state.get('contextQuery'));
    }
    var route = [];
    _.map(filter, function (value, property) {
      return route.push('' + property + '=' + encodeURIComponent(value));
    });
    return route.join(separator);
  },

  getRoute: function () {
    var filter = this.options.app.state.get('filter'),
        query = Controller.prototype.getRoute.apply(this, arguments);
    if (filter != null) {
      if (this.options.app.state.get('changed') && query.length > 0) {
        query = 'id=' + filter.id + '|' + query;
      } else {
        query = 'id=' + filter.id;
      }
    }
    return query;
  },

  _prepareComponent: function (issue) {
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

  showComponentViewer: function (issue) {
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    key.setScope('componentViewer');
    this.options.app.issuesView.unbindScrollEvents();
    this.options.app.state.set('component', this._prepareComponent(issue));
    this.options.app.componentViewer = new ComponentViewer({ app: this.options.app });
    this.options.app.layout.workspaceComponentViewerRegion.show(this.options.app.componentViewer);
    this.options.app.layout.showComponentViewer();
    return this.options.app.componentViewer.openFileByIssue(issue);
  },

  closeComponentViewer: function () {
    key.setScope('list');
    $('body').click();
    this.options.app.state.unset('component');
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    this.options.app.layout.hideComponentViewer();
    this.options.app.issuesView.bindScrollEvents();
    return this.options.app.issuesView.scrollTo();
  },

  showHomePage: function () {
    this.fetchList();
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    key.setScope('home');
    this.options.app.issuesView.unbindScrollEvents();
    this.options.app.homeView = new HomeView({
      app: this.options.app,
      collection: this.options.app.filters
    });
    this.options.app.layout.workspaceHomeRegion.show(this.options.app.homeView);
    return this.options.app.layout.showHomePage();
  },

  hideHomePage: function () {
    this.options.app.layout.workspaceComponentViewerRegion.reset();
    this.options.app.layout.workspaceHomeRegion.reset();
    key.setScope('list');
    this.options.app.layout.hideHomePage();
    this.options.app.issuesView.bindScrollEvents();
    return this.options.app.issuesView.scrollTo();
  }
});


