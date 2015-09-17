import $ from 'jquery';
import Marionette from 'backbone.marionette';
import UpdateView from './update-view';
import DeleteView from './delete-view';
import UsersView from './users-view';
import './templates';

export default Marionette.ItemView.extend({
  tagName: 'li',
  className: 'panel panel-vertical',
  template: Templates['groups-list-item'],

  events: {
    'click .js-group-update': 'onUpdateClick',
    'click .js-group-delete': 'onDeleteClick',
    'click .js-group-users': 'onUsersClick'
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
    this.updateGroup();
  },

  onDeleteClick: function (e) {
    e.preventDefault();
    this.deleteGroup();
  },

  onUsersClick: function (e) {
    e.preventDefault();
    $('.tooltip').remove();
    this.showUsers();
  },

  updateGroup: function () {
    new UpdateView({
      model: this.model,
      collection: this.model.collection
    }).render();
  },

  deleteGroup: function () {
    new DeleteView({ model: this.model }).render();
  },

  showUsers: function () {
    new UsersView({ model: this.model }).render();
  }
});


