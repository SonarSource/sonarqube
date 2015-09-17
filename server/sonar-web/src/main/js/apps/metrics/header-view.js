import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['metrics-header'],

  events: {
    'click #metrics-create': 'onCreateClick'
  },

  onCreateClick: function (e) {
    e.preventDefault();
    this.createMetric();
  },

  createMetric: function () {
    new CreateView({
      collection: this.collection,
      domains: this.options.domains,
      types: this.options.types,
      app: this.options.app
    }).render();
  }
});


