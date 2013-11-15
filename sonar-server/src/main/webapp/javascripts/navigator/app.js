/* global _:false, Backbone:false */

window.SS = typeof window.SS === 'object' ? window.SS : {};

(function() {

  var NavigatorApp = new Backbone.Marionette.Application();
  window.SS.NavigatorApp = NavigatorApp;



  NavigatorApp.addRegions({
    filtersRegion: '.navigator-filters'
  });


  NavigatorApp.addInitializer(function() {
    this.router = new window.SS.NavigatorRouter();
    Backbone.history.start();
  });

})();
