define([
  './facet'
], function (Facet) {

  return Backbone.Collection.extend({
    model: Facet
  });

});
