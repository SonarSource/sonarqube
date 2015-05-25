define([
  './create-view',
  './templates'
], function (CreateView) {

  return Marionette.ItemView.extend({
    template: Templates['provisioning-header'],

    events: {
      'click #provisioning-create': 'onCreateClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.createProject();
    },

    createProject: function () {
      new CreateView({
        collection: this.collection
      }).render();
    }
  });

});
