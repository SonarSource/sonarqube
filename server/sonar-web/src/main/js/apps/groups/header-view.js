import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import Template from './templates/groups-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    'request': 'showSpinner',
    'sync': 'hideSpinner'
  },

  events: {
    'click #groups-create': 'onCreateClick'
  },

  showSpinner: function () {
    this.$('.spinner').removeClass('hidden');
  },

  hideSpinner: function () {
    this.$('.spinner').addClass('hidden');
  },

  onCreateClick: function (e) {
    e.preventDefault();
    this.createGroup();
  },

  createGroup: function () {
    new CreateView({
      collection: this.collection
    }).render();
  }
});


