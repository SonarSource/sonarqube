import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import State from './models/state';
import Layout from './layout';
import Issues from './models/issues';
import Facets from 'components/navigator/models/facets';
import Filters from './models/filters';
import Controller from './controller';
import Router from './router';
import WorkspaceListView from './workspace-list-view';
import WorkspaceHeaderView from './workspace-header-view';
import FacetsView from './facets-view';
import './helpers/format-facet-value';

var App = new Marionette.Application(),
    init = function (options) {
      this.config = options.config;
      this.state = new State({
        isContext: true,
        contextQuery: { componentUuids: options.config.resource },
        contextComponentUuid: options.config.resource,
        contextComponentName: options.config.resourceName,
        contextComponentQualifier: options.config.resourceQualifier
      });
      this.updateContextFacets();
      this.list = new Issues();
      this.facets = new Facets();
      this.filters = new Filters();

      this.layout = new Layout({ app: this, el: options.el });
      this.layout.render();
      $('#footer').addClass('search-navigator-footer');

      this.controller = new Controller({ app: this });

      this.issuesView = new WorkspaceListView({
        app: this,
        collection: this.list
      });
      this.layout.workspaceListRegion.show(this.issuesView);
      this.issuesView.bindScrollEvents();

      this.workspaceHeaderView = new WorkspaceHeaderView({
        app: this,
        collection: this.list
      });
      this.layout.workspaceHeaderRegion.show(this.workspaceHeaderView);

      this.facetsView = new FacetsView({
        app: this,
        collection: this.facets
      });
      this.layout.facetsRegion.show(this.facetsView);

      this.controller.fetchFilters().done(function () {
        key.setScope('list');
        App.router = new Router({ app: App });
        Backbone.history.start();
      });
    };

App.getContextQuery = function () {
  return { componentUuids: this.config.resource };
};

App.getRestrictedFacets = function () {
  return {
    'TRK': ['projectUuids'],
    'BRC': ['projectUuids'],
    'DIR': ['projectUuids', 'moduleUuids', 'directories'],
    'DEV': ['authors'],
    'DEV_PRJ': ['projectUuids', 'authors']
  };
};

App.updateContextFacets = function () {
  var facets = this.state.get('facets'),
      allFacets = this.state.get('allFacets'),
      facetsFromServer = this.state.get('facetsFromServer');
  return this.state.set({
    facets: facets,
    allFacets: _.difference(allFacets, this.getRestrictedFacets()[this.config.resourceQualifier]),
    facetsFromServer: _.difference(facetsFromServer, this.getRestrictedFacets()[this.config.resourceQualifier])
  });
};

App.on('start', function (options) {
  $.when(window.requestMessages()).done(function () {
    init.call(App, options);
  });
});

export default App;


