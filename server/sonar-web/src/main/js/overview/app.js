requirejs([
  'overview/layout'
], function (Layout) {

  var App = new Marionette.Application();

  App.addInitializer(function () {
    this.layout = new Layout({ el: '#overview' });
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
