import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Layout from './layout';
import HeaderView from './header-view';
import SearchView from './search-view';
import ListView from './list-view';
import FooterView from './footer-view';
import Controller from './controller';
import Router from './router';
import Plugins from './plugins';

var App = new Marionette.Application(),
    init = function (options) {
      // State
      this.state = new Backbone.Model({
        updateCenterActive: window.SS.updateCenterActive
      });

      // Layout
      this.layout = new Layout({ el: options.el });
      this.layout.render();

      // Plugins
      this.plugins = new Plugins();

      // Controller
      this.controller = new Controller({ collection: this.plugins, state: this.state });

      // Router
      this.router = new Router({ controller: this.controller });

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
        root: options.urlRoot
      });
    };

App.on('start', function (options) {
  window.requestMessages().done(function () {
    init.call(App, options);
  });
});

export default App;


