requirejs([
  'overview/layout',
  'overview/models/state'
], function (Layout, State) {

  var App = new Marionette.Application();

  App.addInitializer(function () {
    this.state = new State();
    this.layout = new Layout({
      el: '.overview',
      model: this.state
    }).render();
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
