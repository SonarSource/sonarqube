import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/update-center-header.hbs';

export default Marionette.ItemView.extend({
  template: Template,

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


