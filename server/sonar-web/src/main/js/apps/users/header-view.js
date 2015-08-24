define([
  'backbone.marionette',
  './create-view',
  './templates'
], function (Marionette, CreateView) {

  return Marionette.ItemView.extend({
    template: Templates['users-header'],

    events: {
      'click #users-create': 'onCreateClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.createUser();
    },

    createUser: function () {
      new CreateView({
        collection: this.collection
      }).render();
    }
  });

});
