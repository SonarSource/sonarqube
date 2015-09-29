import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'property',

  defaults: {
    enabled: false
  },

  getValues: function () {
    return this.get('values') || [];
  },

  toggle: function () {
    var enabled = this.get('enabled');
    this.set({ enabled: !enabled });
  }
});


