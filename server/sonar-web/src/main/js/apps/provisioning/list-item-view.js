import Marionette from 'backbone.marionette';
import DeleteView from './delete-view';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Templates['provisioning-list-item'],

  modelEvents: {
    'change:selected': 'onSelectedChange'
  },

  events: {
    'click .js-toggle': 'onToggleClick',
    'click .js-project-delete': 'onDeleteClick'
  },

  onRender: function () {
    this.$el.attr('data-id', this.model.id);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body', placement: 'bottom' });
  },

  onDestroy: function () {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  onToggleClick: function (e) {
    e.preventDefault();
    this.toggle();
  },

  onDeleteClick: function (e) {
    e.preventDefault();
    this.deleteProject();
  },

  onSelectedChange: function () {
    this.$('.js-toggle').toggleClass('icon-checkbox-checked', this.model.get('selected'));
  },

  toggle: function () {
    this.model.toggle();
  },

  deleteProject: function () {
    new DeleteView({ model: this.model }).render();
  }
});


