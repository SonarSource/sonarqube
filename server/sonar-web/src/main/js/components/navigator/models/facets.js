define([
  'backbone',
  'components/navigator/models/facet'
], function (Backbone, Facet) {

  return Backbone.Collection.extend({
    model: Facet
  });

});
