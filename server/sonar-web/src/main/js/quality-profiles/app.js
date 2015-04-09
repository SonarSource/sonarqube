require([], function () {

  var $ = jQuery,
      App = new Marionette.Application();

  window.requestMessages().done(function () {
    App.start();
  });

});
