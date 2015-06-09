define([
  './update-view',
  './delete-view',
  './templates'
], function (UpdateView, DeleteView) {

  return Marionette.ItemView.extend({
    tagName: 'li',
    className: 'panel panel-vertical',
    template: Templates['metrics-list-item'],

    events: {
      'click .js-metric-update': 'onUpdateClick',
      'click .js-metric-delete': 'onDeleteClick'
    },

    onRender: function () {
      this.$el
          .attr('data-id', this.model.id)
          .attr('data-key', this.model.get('key'));
      this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
    },

    onClose: function () {
      this.$('[data-toggle="tooltip"]').tooltip('destroy');
    },

    onUpdateClick: function (e) {
      e.preventDefault();
      this.updateMetric();
    },

    onDeleteClick: function (e) {
      e.preventDefault();
      this.deleteMetric();
    },

    updateMetric: function () {
      new UpdateView({
        model: this.model,
        collection: this.model.collection,
        types: this.options.types,
        domains: this.options.domains
      }).render();
    },

    deleteMetric: function () {
      new DeleteView({ model: this.model }).render();
    }
  });

});
