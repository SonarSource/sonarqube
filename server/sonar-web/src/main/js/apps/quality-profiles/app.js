define([
  'backbone',
  'backbone.marionette',
  './router',
  './controller',
  './layout',
  './profiles',
  './actions-view',
  './profiles-view'
], function (Backbone, Marionette, Router, Controller, Layout, Profiles, ActionsView, ProfilesView) {

  var $ = jQuery,
      App = new Marionette.Application(),
      requestExporters = $.get(baseUrl + '/api/qualityprofiles/exporters').done(function (r) {
        App.exporters = r.exporters;
      }),
      init = function (options) {
        App.canWrite = options.user.permissions.global.indexOf('profileadmin') !== -1;

        // Layout
        this.layout = new Layout({ el: options.el });
        this.layout.render();
        $('#footer').addClass('search-navigator-footer');

        // Profiles List
        this.profiles = new Profiles();

        // Controller
        this.controller = new Controller({ app: this });

        // Actions View
        this.actionsView = new ActionsView({
          collection: this.profiles,
          canWrite: this.canWrite
        });
        this.actionsView.requestLanguages().done(function () {
          App.layout.actionsRegion.show(App.actionsView);
        });

        // Profiles View
        this.profilesView = new ProfilesView({
          collection: this.profiles,
          canWrite: this.canWrite
        });
        this.layout.resultsRegion.show(this.profilesView);

        // Router
        this.router = new Router({ app: this });
        Backbone.history.start({
          pushState: true,
          root: options.urlRoot
        });
      };

  App.on('start', function (options) {
    $.when(requestExporters).done(function () {
      init.call(App, options);
    });
  });

  return App;

});
