define([
  './router',
  './controller',
  './layout',
  './list',
  './list-view',
  './filters-view',
  './search-view'
], function (Router, Controller, Layout, List, ListView, FiltersView, SearchView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {
        // State
        this.state = new Backbone.Model({ internal: false });
        this.state.match = function (test) {
          var pattern = new RegExp(this.get('query'), 'i');
          return test.search(pattern) !== -1;
        };

        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();
        $('#footer').addClass('search-navigator-footer');

        // Web Services List
        this.list = new List();

        // Controller
        this.controller = new Controller({
          app: this,
          state: this.state
        });

        // List View
        this.listView = new ListView({
          collection: this.list,
          state: this.state
        });
        this.layout.resultsRegion.show(this.listView);

        // Filters View
        this.filtersView = new FiltersView({
          collection: this.list,
          state: this.state
        });
        this.layout.actionsRegion.show(this.filtersView);

        // Search View
        this.searchView = new SearchView({
          state: this.state
        });
        this.layout.searchRegion.show(this.searchView);

        // Router
        this.router = new Router({ app: this });
        Backbone.history.start({
          pushState: true,
          root: options.urlRoot
        });
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  return App;

});
