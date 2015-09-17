import $ from 'jquery';
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
import FiltersView from './filters-view';
import './helpers/format-facet-value';

var App = new Marionette.Application(),
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
  $.when(window.requestMessages()).done(function () {
    init.call(App, options);
  });
});

export default App;


