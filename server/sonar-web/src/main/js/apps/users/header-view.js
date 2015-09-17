import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import './templates';

export default Marionette.ItemView.extend({
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


