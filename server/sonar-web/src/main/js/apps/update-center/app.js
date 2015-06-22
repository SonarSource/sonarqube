define([
  './layout',
  './header-view',
  './search-view',
  './list-view',
  './footer-view',
  './controller',
  './router',
  './plugins'
], function (Layout, HeaderView, SearchView, ListView, FooterView, Controller, Router, Plugins) {

  var App = new Marionette.Application(),
      init = function (options) {
        // State
        this.state = new Backbone.Model();

        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();

        // Plugins
        this.plugins = new Plugins();

        // Controller
        this.controller = new Controller({ collection: this.plugins, state: this.state });

        // Router
        this.router = new Router({ controller: this.controller});

        // Header
        this.headerView = new HeaderView({ collection: this.plugins });
        this.layout.headerRegion.show(this.headerView);

        // Search
        this.searchView = new SearchView({ collection: this.plugins, router: this.router, state: this.state });
        this.layout.searchRegion.show(this.searchView);
        this.searchView.focusSearch();

        // List
        this.listView = new ListView({ collection: this.plugins });
        this.layout.listRegion.show(this.listView);

        // Footer
        this.footerView = new FooterView({ collection: this.plugins });
        this.layout.footerRegion.show(this.footerView);

        // Go
        Backbone.history.start({
          pushState: true,
          root: options.urlRoot || (baseUrl + '/updatecenter_new')
        });
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  return App;

});
