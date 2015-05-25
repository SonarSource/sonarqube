define([
  './delete-view',
  './templates'
], function (DeleteView) {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'panel panel-vertical',
    template: Templates['provisioning-list-item'],

    events: {
      'click .js-project-delete': 'onDeleteClick'
    },

    onRender: function () {
      this.$el.attr('data-id', this.model.id);
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onDeleteClick: function (e) {
      e.preventDefault();
      this.deleteProject();
    },

    deleteProject: function () {
      new DeleteView({ model: this.model }).render();
    }
  });

});
