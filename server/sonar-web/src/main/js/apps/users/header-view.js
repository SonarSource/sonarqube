import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import Template from './templates/users-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    'request': 'showSpinner',
    'sync': 'hideSpinner'
  },

  events: {
    'click #users-create': 'onCreateClick'
  },

  showSpinner: function () {
    this.$('.spinner').removeClass('hidden');
  },

  hideSpinner: function () {
    this.$('.spinner').addClass('hidden');
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


