requirejs.config({
  baseUrl: `${baseUrl}/js`,

  paths: {
    'backbone': 'third-party/backbone',
    'backbone.marionette': 'third-party/backbone.marionette',
    'handlebars': 'third-party/handlebars'
  },

  shim: {
    'backbone.marionette': {
      deps: ['backbone'],
      exports: 'Marionette'
    },
    'backbone': {
      exports: 'Backbone'
    },
    'handlebars': {
      exports: 'Handlebars'
    }
  }
});


requirejs([
  'backbone',
  'backbone.marionette',
  'dashboard/collections/widgets',
  'dashboard/views/widgets-view',
  'dashboard/mockjax',
  'common/handlebars-extensions'
], function (Backbone, Marionette, Widgets, WidgetsView) {

  var App = new Marionette.Application(),
      $ = jQuery;

  App.addInitializer(function () {
    this.widgetsView = new WidgetsView({
      collection: this.widgets,
      dashboard: this.dashboard,
      el: $('#dashboard')
    });
    this.widgetsView.render();
  });

  var requestDetails = function () {
    return $.get(`${baseUrl}/api/dashboards/details`, { did: window.did }, function (data) {
      console.log(JSON.stringify(data));
      App.dashboard = new Backbone.Model(_.omit(data, 'widgets'));
      App.widgets = new Widgets(data.widgets);
    });
  };

  $.when(requestDetails(), window.requestMessages()).done(function () {
    App.start();
  });

});
