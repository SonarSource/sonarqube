import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['groups-header'],

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


