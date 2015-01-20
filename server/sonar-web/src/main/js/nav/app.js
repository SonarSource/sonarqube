define([
  'nav/global-navbar-view',
  'nav/context-navbar-view'
], function (GlobalNavbarView, ContextNavbarView) {

  var $ = jQuery,
      App = new Marionette.Application();

  App.addInitializer(function () {
    this.navbarView = new GlobalNavbarView({
      app: App,
      el: $('.navbar-global'),
      collection: new Backbone.Collection(window.navbarGlobalMenu)
    });
    this.navbarView.render();
  });

  if (window.navbarBreadcrumbs != null) {
    App.addInitializer(function () {
      this.contextNavbarView = new ContextNavbarView({
        app: App,
        el: $('.navbar-context'),
        collection: new Backbone.Collection(window.navbarContextMenu),
        breadcrumbs: new Backbone.Collection(window.navbarBreadcrumbs),
      });
      this.contextNavbarView.render();
    });
  }

  window.requestMessages().done(function () {
    App.start();
  });

});
