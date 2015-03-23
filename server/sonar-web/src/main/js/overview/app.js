requirejs([
  'overview/layout',
  'overview/models/state',
  'overview/views/issues-view',
  'overview/views/coverage-view'
], function (Layout, State, IssuesView, CoverageView) {

  var App = new Marionette.Application();

  App.addInitializer(function () {
    this.state = new State();
    this.layout = new Layout({
      el: '.overview',
      model: this.state
    }).render();
    this.layout.issuesRegion.show(new IssuesView());
    this.layout.coverageRegion.show(new CoverageView());
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
