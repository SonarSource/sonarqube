Handlebars.registerHelper('formatFacetValue', function (value, facetMode) {
  var formatter = facetMode === 'debt' ? 'SHORT_WORK_DUR' : 'SHORT_INT';
  return window.formatMeasure(value, formatter);
});


