define([
  './layout',
  './projects',
  './header-view',
  './search-view',
  './list-view',
  './list-footer-view'
], function (Layout, Projects, HeaderView, SearchView, ListView, ListFooterView) {

  var App = new Marionette.Application(),
      init = function (options) {
        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();

        // Collection
        this.projects = new Projects();

        // Header View
        this.headerView = new HeaderView({ collection: this.projects });
        this.layout.headerRegion.show(this.headerView);

        // Search View
        this.searchView = new SearchView({ collection: this.projects });
        this.layout.searchRegion.show(this.searchView);

        // List View
        this.listView = new ListView({ collection: this.projects });
        this.layout.listRegion.show(this.listView);

        // List Footer View
        this.listFooterView = new ListFooterView({ collection: this.projects });
        this.layout.listFooterRegion.show(this.listFooterView);

        // Go!
        this.projects.fetch();
      };

  App.on('start', function (options) {
    window.requestMessages().done(function () {
      init.call(App, options);
    });
  });

  return App;

});
