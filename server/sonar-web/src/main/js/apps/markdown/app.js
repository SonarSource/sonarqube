import Marionette from 'backbone.marionette';
import MarkdownView from './markdown-help-view';

var App = new Marionette.Application();

App.on('start', function (options) {
  new MarkdownView({ el: options.el }).render();
});

export default App;


