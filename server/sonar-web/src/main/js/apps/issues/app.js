define([
  'backbone',
  'backbone.marionette',
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
  './filters-view',
  './helpers/format-facet-value'
], function (Backbone, Marionette, State, Layout, Issues, Facets, Filters, Controller, Router, WorkspaceListView,
             WorkspaceHeaderView, FacetsView, FiltersView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {
        this.state = new State();
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

        this.filtersView = new FiltersView({
          app: this,
          collection: this.filters
        });
        this.layout.filtersRegion.show(this.filtersView);

        this.controller.fetchFilters().done(function () {
          key.setScope('list');
          App.router = new Router({ app: App });
          Backbone.history.start();
        });
      };

  App.on('start', function (options) {
    init.call(App, options);
  });

  return App;

});
