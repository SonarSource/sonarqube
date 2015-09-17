import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['update-center-header'],

  collectionEvents: {
    all: 'render'
  },

  events: {
    'click .js-cancel-all': 'cancelAll'
  },

  cancelAll: function () {
    this.collection.cancelAll();
  },

  serializeData: function () {
    return _.extend(this._super(), {
      installing: this.collection._installedCount,
      uninstalling: this.collection._uninstalledCount
    });
  }
});


