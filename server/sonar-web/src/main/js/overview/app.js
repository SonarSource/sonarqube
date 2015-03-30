requirejs([
  'overview/layout',
  'overview/models/state',
  'overview/views/gate-view',
  'overview/views/size-view',
  'overview/views/issues-view',
  'overview/views/debt-view',
  'overview/views/coverage-view',
  'overview/views/duplications-view'
], function (Layout,
             State,
             GateView,
             SizeView,
             IssuesView,
             DebtView,
             CoverageView,
             DuplicationsView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addInitializer(function () {
    $('body').addClass('dashboard-page');
    this.state = new State(window.overviewConf);
    this.layout = new Layout({
      el: '.overview',
      model: this.state
    }).render();
    this.layout.gateRegion.show(new GateView({ model: this.state }));
    this.layout.sizeRegion.show(new SizeView({ model: this.state }));
    this.layout.issuesRegion.show(new IssuesView({ model: this.state }));
    this.layout.debtRegion.show(new DebtView({ model: this.state }));
    this.layout.coverageRegion.show(new CoverageView({ model: this.state }));
    this.layout.duplicationsRegion.show(new DuplicationsView({ model: this.state }));
    this.state.fetch();
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
