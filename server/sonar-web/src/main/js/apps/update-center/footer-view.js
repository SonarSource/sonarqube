import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['update-center-footer'],

  collectionEvents: {
    'all': 'render'
  },

  serializeData: function () {
    return _.extend(this._super(), {
      total: this.collection.where({ _hidden: false }).length
    });
  }
});


