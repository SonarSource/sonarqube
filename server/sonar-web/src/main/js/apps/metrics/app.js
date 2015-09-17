import $ from 'jquery';
import Marionette from 'backbone.marionette';
import Layout from './layout';
import Metrics from './metrics';
import HeaderView from './header-view';
import ListView from './list-view';
import ListFooterView from './list-footer-view';

var App = new Marionette.Application(),
    init = function (options) {
      // Layout
      this.layout = new Layout({ el: options.el });
      this.layout.render();

      // Collection
      this.metrics = new Metrics();

      // Header View
      this.headerView = new HeaderView({
        collection: this.metrics,
        domains: this.domains,
        types: this.types,
        app: App
      });
      this.layout.headerRegion.show(this.headerView);

      // List View
      this.listView = new ListView({
        collection: this.metrics,
        domains: this.domains,
        types: this.types
      });
      this.layout.listRegion.show(this.listView);

      // List Footer View
      this.listFooterView = new ListFooterView({ collection: this.metrics });
      this.layout.listFooterRegion.show(this.listFooterView);

      // Go!
      this.metrics.fetch();
    };


App.requestDomains = function () {
  return $.get(baseUrl + '/api/metrics/domains').done(function (r) {
    App.domains = r.domains;
  });
};
App.requestTypes = function () {
  return $.get(baseUrl + '/api/metrics/types').done(function (r) {
    App.types = r.types;
  });
};

App.on('start', function (options) {
  $.when(window.requestMessages(), App.requestDomains(), App.requestTypes()).done(function () {
    init.call(App, options);
  });
});

export default App;


