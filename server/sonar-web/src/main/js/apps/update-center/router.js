import Backbone from 'backbone';

export default Backbone.Router.extend({
  routes: {
    '': 'index',
    'installed': 'showInstalled',
    'updates': 'showUpdates',
    'available': 'showAvailable',
    'system': 'showSystemUpgrades'
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
  },

  showSystemUpgrades: function () {
    this.controller.showSystemUpgrades();
  }
});


