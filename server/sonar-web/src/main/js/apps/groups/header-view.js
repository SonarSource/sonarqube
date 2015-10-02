import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import Template from './templates/groups-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #groups-create': 'onCreateClick'
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


