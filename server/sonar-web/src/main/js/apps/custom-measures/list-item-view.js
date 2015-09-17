import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import DeleteView from './delete-view';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Templates['custom-measures-list-item'],

  events: {
    'click .js-custom-measure-update': 'onUpdateClick',
    'click .js-custom-measure-delete': 'onDeleteClick'
  },

  onRender: function () {
    this.$el.attr('data-id', this.model.id);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onUpdateClick: function (e) {
    e.preventDefault();
    this.updateCustomMeasure();
  },

  onDeleteClick: function (e) {
    e.preventDefault();
    this.deleteCustomMeasure();
  },

  updateCustomMeasure: function () {
    new UpdateView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteCustomMeasure: function () {
    new DeleteView({ model: this.model }).render();
  }
});


