define([
  './condition'
], function (Condition) {

  return Backbone.Collection.extend({
    model: Condition,
    comparator: 'metric'
  });

});
