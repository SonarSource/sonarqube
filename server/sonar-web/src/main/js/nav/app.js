define([
  'nav/navbar'
], function (NavbarView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addInitializer(function () {
    this.navbarView = new NavbarView({
      app: App,
      el: $('.navbar'),
      collection: new Backbone.Collection(window.navbarItems)
    });
    this.navbarView.render();
  });

  window.requestMessages().done(function () {
    App.start();
  });

});
