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
  './facets-view',
  './filters-view'
], function (State, Layout, Issues, Facets, Filters, Controller, Router, WorkspaceListView, WorkspaceHeaderView,
             FacetsView, FiltersView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addInitializer(function () {
    this.state = new State();
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
    this.filtersView = new FiltersView({
      app: this,
      collection: this.filters
    });
    this.layout.filtersRegion.show(this.filtersView);
  });

  App.addInitializer(function () {
    this.controller.fetchFilters().done(function () {
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
