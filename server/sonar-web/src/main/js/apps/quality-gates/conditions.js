define([
  'backbone',
  './condition'
], function (Backbone, Condition) {

  return Backbone.Collection.extend({
    model: Condition,
    comparator: 'metric'
  });

});
