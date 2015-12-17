import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from './templates/update-center-footer.hbs';

export default Marionette.ItemView.extend({
  template: Template,

  collectionEvents: {
    'all': 'render'
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      total: this.collection.where({ _hidden: false }).length
    });
  }
});


