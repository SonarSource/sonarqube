define([
  './router',
  './layout',
  './reports',
  './header-view',
  './search-view',
  './list-view',
  './list-footer-view'
], function (Router, Layout, Reports, HeaderView, SearchView, ListView, ListFooterView) {

  var App = new Marionette.Application(),
      init = function (options) {
        // Collection
        this.reports = new Reports();

        // Router
        this.router = new Router({ reports: this.reports });

        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();

        // Header View
        this.headerView = new HeaderView({ collection: this.reports });
        this.layout.headerRegion.show(this.headerView);

        // Search View
        this.searchView = new SearchView({
          collection: this.reports,
          router: this.router
        });
        this.layout.searchRegion.show(this.searchView);

        // List View
        this.listView = new ListView({ collection: this.reports });
        this.layout.listRegion.show(this.listView);

        // List Footer View
        this.listFooterView = new ListFooterView({ collection: this.reports });
        this.layout.listFooterRegion.show(this.listFooterView);

        // Go!
        Backbone.history.start({
          pushState: true,
          root: getRoot()
        });
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  function getRoot () {
    var COMPUTATION = '/computation',
        path = window.location.pathname,
        pos = path.indexOf(COMPUTATION);
    return path.substr(0, pos + COMPUTATION.length);
  }

  return App;

});
