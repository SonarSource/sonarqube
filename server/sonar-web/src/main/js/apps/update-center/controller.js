import Marionette from 'backbone.marionette';

export default Marionette.Controller.extend({
  initialize: function (options) {
    this.collection = options.collection;
    this.state = options.state;
  },

  showInstalled: function () {
    this.state.set({ section: 'installed' });
    this.collection.fetchInstalled();
  },

  showUpdates: function () {
    this.state.set({ section: 'updates' });
    this.collection.fetchUpdates();
  },

  showAvailable: function () {
    this.state.set({ section: 'available' });
    this.collection.fetchAvailable();
  },

  showSystemUpgrades: function () {
    this.state.set({ section: 'system' });
    this.collection.fetchSystemUpgrades();
  }
});


