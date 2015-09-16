define(['./markdown-help-view'], function (MarkdownView) {

  var App = new Marionette.Application();

  App.on('start', function (options) {
    new MarkdownView({ el: options.el }).render();
  });

  return App;

});
