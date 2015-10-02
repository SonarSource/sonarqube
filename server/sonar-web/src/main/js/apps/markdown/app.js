import Marionette from 'backbone.marionette';
import MarkdownView from './markdown-help-view';

var App = new Marionette.Application();

App.on('start', function () {
  let options = window.sonarqube;
  new MarkdownView({ el: options.el }).render();
});

window.sonarqube.appStarted.then(options => App.start(options));


