define([
  './filter'
], function (Filter) {

  return Backbone.Collection.extend({
    model: Filter
  });

});
