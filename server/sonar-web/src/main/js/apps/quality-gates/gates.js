define([
  './gate'
], function (Gate) {

  return Backbone.Collection.extend({
    model: Gate,

    url: function () {
      return baseUrl + '/api/qualitygates/list';
    },

    parse: function (r) {
      return r.qualitygates.map(function (gate) {
        return _.extend(gate, { isDefault: gate.id === r.default });
      });
    },

    comparator: function (item) {
      return item.get('name').toLowerCase();
    },

    toggleDefault: function (gate) {
      var isDefault = gate.isDefault();
      this.forEach(function (model) {
        model.set({ isDefault: gate.id === model.id ? !isDefault : false });
      });
    }
  });

});
