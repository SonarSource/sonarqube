define([
  'backbone.marionette',
  './markdown-help-view'
], function (Marionette, MarkdownView) {

  var App = new Marionette.Application();

  App.on('start', function (options) {
    new MarkdownView({ el: options.el }).render();
  });

  return App;

});
