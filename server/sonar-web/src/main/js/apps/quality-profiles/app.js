import $ from 'jquery';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Router from './router';
import Controller from './controller';
import Layout from './layout';
import Profiles from './profiles';
import ActionsView from './actions-view';
import ProfilesView from './profiles-view';

var App = new Marionette.Application(),
    requestUser = $.get(baseUrl + '/api/users/current').done(function (r) {
      App.canWrite = r.permissions.global.indexOf('profileadmin') !== -1;
    }),
    requestExporters = $.get(baseUrl + '/api/qualityprofiles/exporters').done(function (r) {
      App.exporters = r.exporters;
    }),
    init = function (options) {
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
  $.when(window.requestMessages(), requestUser, requestExporters).done(function () {
    init.call(App, options);
  });
});

export default App;


