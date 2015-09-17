import $ from 'jquery';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import Gates from './gates';
import GatesView from './gates-view';
import ActionsView from './actions-view';
import Router from './router';
import Layout from './layout';
import Controller from './controller';

var App = new Marionette.Application();

var init = function (options) {
  // Layout
  this.layout = new Layout({ el: options.el });
  this.layout.render();
  $('#footer').addClass('search-navigator-footer');

  // Gates List
  this.gates = new Gates();

  // Controller
  this.controller = new Controller({ app: this });

  // Header
  this.actionsView = new ActionsView({
    canEdit: this.canEdit,
    collection: this.gates
  });
  this.layout.actionsRegion.show(this.actionsView);

  // List
  this.gatesView = new GatesView({
    canEdit: this.canEdit,
    collection: this.gates
  });
  this.layout.resultsRegion.show(this.gatesView);

  // Router
  this.router = new Router({ app: this });
  Backbone.history.start({
    pushState: true,
    root: options.urlRoot
  });
};

var appXHR = $.get(baseUrl + '/api/qualitygates/app')
    .done(function (r) {
      App.canEdit = r.edit;
      App.periods = r.periods;
      App.metrics = r.metrics;
    });

App.on('start', function (options) {
  $.when(window.requestMessages(), appXHR).done(function () {
    init.call(App, options);
  });
});

export default App;


