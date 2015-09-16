define([
  'components/navigator/models/facet'
], function (Facet) {

  return Backbone.Collection.extend({
    model: Facet
  });

});
