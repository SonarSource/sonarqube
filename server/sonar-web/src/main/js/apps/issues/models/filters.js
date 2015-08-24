define([
  'backbone',
  './filter'
], function (Backbone, Filter) {

  return Backbone.Collection.extend({
    model: Filter
  });

});
