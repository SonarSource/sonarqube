define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    template: Templates['update-center-header'],

    collectionEvents: {
      all: 'render'
    },

    events: {
      'click .js-cancel-all': 'cancelAll'
    },

    cancelAll: function () {
      this.collection.cancelAll();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        installing: this.collection._installedCount,
        uninstalling: this.collection._uninstalledCount
      });
    }
  });

});
