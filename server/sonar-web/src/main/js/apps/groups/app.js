import Marionette from 'backbone.marionette';
import Layout from './layout';
import Groups from './groups';
import HeaderView from './header-view';
import SearchView from './search-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';

var App = new Marionette.Application(),
    init = function (options) {
      // Layout
      this.layout = new Layout({ el: options.el });
      this.layout.render();

      // Collection
      this.groups = new Groups();

      // Header View
      this.headerView = new HeaderView({ collection: this.groups });
      this.layout.headerRegion.show(this.headerView);

      // Search View
      this.searchView = new SearchView({ collection: this.groups });
      this.layout.searchRegion.show(this.searchView);

      // List View
      this.listView = new ListView({ collection: this.groups });
      this.layout.listRegion.show(this.listView);

      // List Footer View
      this.listFooterView = new ListFooterView({ collection: this.groups });
      this.layout.listFooterRegion.show(this.listFooterView);

      // Go!
      this.groups.fetch();
    };

App.on('start', function (options) {
  window.requestMessages().done(function () {
    init.call(App, options);
  });
});

export default App;


