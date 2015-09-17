import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Router from './router';
import Layout from './layout';
import Reports from './reports';
import HeaderView from './header-view';
import SearchView from './search-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';

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
        root: options.urlRoot
      });
    };

App.on('start', function (options) {
  window.requestMessages().done(function () {
    init.call(App, options);
  });
});

export default App;


