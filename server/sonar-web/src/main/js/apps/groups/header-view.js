define([
  'backbone.marionette',
  './create-view',
  './templates'
], function (Marionette, CreateView) {

  return Marionette.ItemView.extend({
    template: Templates['groups-header'],

    events: {
      'click #groups-create': 'onCreateClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.createGroup();
    },

    createGroup: function () {
      new CreateView({
        collection: this.collection
      }).render();
    }
  });

});
