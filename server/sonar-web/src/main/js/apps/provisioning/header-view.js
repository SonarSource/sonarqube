define([
  'backbone.marionette',
  './create-view',
  './bulk-delete-view',
  './templates'
], function (Marionette, CreateView, BulkDeleteView) {

  return Marionette.ItemView.extend({
    template: Templates['provisioning-header'],

    collectionEvents: {
      'change:selected': 'toggleDeleteButton',
      'reset': 'toggleDeleteButton'
    },

    events: {
      'click #provisioning-create': 'onCreateClick',
      'click #provisioning-bulk-delete': 'onBulkDeleteClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.createProject();
    },

    onBulkDeleteClick: function (e) {
      e.preventDefault();
      this.bulkDelete();
    },

    createProject: function () {
      new CreateView({
        collection: this.collection
      }).render();
    },

    bulkDelete: function () {
      new BulkDeleteView({
        collection: this.collection
      }).render();
    },

    toggleDeleteButton: function () {
      var selectedCount = this.collection.where({ selected: true }).length,
          someSelected = selectedCount > 0;
      this.$('#provisioning-bulk-delete').prop('disabled', !someSelected);
    }
  });

});
