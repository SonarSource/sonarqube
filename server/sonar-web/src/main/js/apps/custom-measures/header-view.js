import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import Template from './templates/custom-measures-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  events: {
    'click #custom-measures-create': 'onCreateClick'
  },

  onCreateClick: function (e) {
    e.preventDefault();
    this.createCustomMeasure();
  },

  createCustomMeasure: function () {
    new CreateView({
      collection: this.collection,
      projectId: this.options.projectId
    }).render();
  }
});


