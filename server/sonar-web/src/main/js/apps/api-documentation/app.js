define([
  'backbone',
  'backbone.marionette',
  './router',
  './controller',
  './layout',
  './list',
  './list-view',
  './filters-view'
], function (Backbone, Marionette, Router, Controller, Layout, List, ListView, FiltersView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      init = function (options) {
        // State
        this.state = new Backbone.Model({ internal: false });

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

        // Router
        this.router = new Router({ app: this });
        Backbone.history.start({
          pushState: true,
          root: options.urlRoot
        });
      };

  App.on('start', function (options) {
    init.call(App, options);
  });

  return App;

});
