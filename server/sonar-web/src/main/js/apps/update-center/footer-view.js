import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/update-center-footer.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    'all': 'render'
  },

  serializeData: function () {
    return _.extend(this._super(), {
      total: this.collection.where({ _hidden: false }).length
    });
  }
});


