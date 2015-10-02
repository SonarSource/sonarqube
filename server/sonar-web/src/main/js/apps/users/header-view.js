import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import Template from './templates/users-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

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


