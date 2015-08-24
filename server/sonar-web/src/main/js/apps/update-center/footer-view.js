define([
  'backbone.marionette',
  './templates'
], function (Marionette) {

  return Marionette.ItemView.extend({
    template: Templates['update-center-footer'],

    collectionEvents: {
      'all': 'render'
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        total: this.collection.where({ _hidden: false }).length
      });
    }
  });

});
