define([
  'coding-rules/facets/custom-labels-facet'
], function (CustomLabelsFacet) {

  return CustomLabelsFacet.extend({

    getLabelsSource: function () {
      return this.options.app.languages;
    }

  });

});
