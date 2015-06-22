define(function () {

  return Backbone.Router.extend({
    routes: {
      '': 'index',
      'installed': 'showInstalled',
      'updates': 'showUpdates',
      'available': 'showAvailable'
    },

    initialize: function (options) {
      this.controller = options.controller;
    },

    index: function () {
      this.navigate('installed', { trigger: true, replace: true });
    },

    showInstalled: function () {
      this.controller.showInstalled();
    },

    showUpdates: function () {
      this.controller.showUpdates();
    },

    showAvailable: function () {
      this.controller.showAvailable();
    }
  });

});
