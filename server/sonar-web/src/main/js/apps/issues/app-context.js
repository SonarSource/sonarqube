define([
  './models/state',
  './layout',
  './models/issues',
  'components/navigator/models/facets',
  './models/filters',
  './controller',
  './router',
  './workspace-list-view',
  './workspace-header-view',
  './facets-view'
], function (State, Layout, Issues, Facets, Filters, Controller, Router, WorkspaceListView, WorkspaceHeaderView,
             FacetsView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.getContextQuery = function () {
    return { componentUuids: window.config.resource };
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
      allFacets: _.difference(allFacets, this.getRestrictedFacets()[window.config.resourceQualifier]),
      facetsFromServer: _.difference(facetsFromServer, this.getRestrictedFacets()[window.config.resourceQualifier])
    });
  };

  App.addInitializer(function () {
    this.state = new State({
      isContext: true,
      contextQuery: this.getContextQuery(),
      contextComponentUuid: window.config.resource,
      contextComponentName: window.config.resourceName,
      contextComponentQualifier: window.config.resourceQualifier
    });
    this.updateContextFacets();
    this.list = new Issues();
    this.facets = new Facets();
    this.filters = new Filters();
  });

  App.addInitializer(function () {
    this.layout = new Layout({ app: this });
    $('.issues').empty().append(this.layout.render().el);
    $('#footer').addClass('search-navigator-footer');
  });

  App.addInitializer(function () {
    this.controller = new Controller({ app: this });
  });

  App.addInitializer(function () {
    this.issuesView = new WorkspaceListView({
      app: this,
      collection: this.list
    });
    this.layout.workspaceListRegion.show(this.issuesView);
    this.issuesView.bindScrollEvents();
  });

  App.addInitializer(function () {
    this.workspaceHeaderView = new WorkspaceHeaderView({
      app: this,
      collection: this.list
    });
    this.layout.workspaceHeaderRegion.show(this.workspaceHeaderView);
  });

  App.addInitializer(function () {
    this.facetsView = new FacetsView({
      app: this,
      collection: this.facets
    });
    this.layout.facetsRegion.show(this.facetsView);
  });

  App.addInitializer(function () {
    return this.controller.fetchFilters().done(function () {
      key.setScope('list');
      App.router = new Router({ app: App });
      Backbone.history.start();
    });
  });

  var l10nXHR = window.requestMessages();
  return jQuery.when(l10nXHR).done(function () {
    return App.start();
  });

});
