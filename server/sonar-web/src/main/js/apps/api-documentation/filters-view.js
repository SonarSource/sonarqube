import _ from 'underscore';
import Marionette from 'backbone.marionette';
import './templates';

export default Marionette.ItemView.extend({
  template: Templates['api-documentation-filters'],

  events: {
    'change .js-toggle-internal': 'toggleInternal'
  },

  initialize: function () {
    this.listenTo(this.options.state, 'change:internal', this.render);
  },

  toggleInternal: function () {
    this.options.state.set({ internal: !this.options.state.get('internal') });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      state: this.options.state.toJSON()
    });
  }
});


