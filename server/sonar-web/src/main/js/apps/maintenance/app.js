import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import MainView from './main-view';
import './partials';
import '../../helpers/handlebars-helpers';

var App = new Marionette.Application();

App.on('start', function () {
  let options = window.sonarqube;

  var viewOptions = _.extend(options, {
    model: new Backbone.Model()
  });
  var mainView = new MainView(viewOptions);
  mainView.render().refresh();
});

App.start();


