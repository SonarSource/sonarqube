import _ from 'underscore';
import Marionette from 'backbone.marionette';
import CreateView from './create-view';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['quality-gate-actions'],

  events: {
    'click #quality-gate-add': 'add'
  },

  add: function (e) {
    e.preventDefault();
    new CreateView({
      collection: this.collection
    }).render();
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canEdit: this.options.canEdit
    });
  }
});


