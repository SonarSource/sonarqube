import Handlebars from 'hbsfy/runtime';
import { formatMeasure } from '../../../helpers/measures';

Handlebars.registerHelper('formatFacetValue', function (value, facetMode) {
  var formatter = facetMode === 'debt' ? 'SHORT_WORK_DUR' : 'SHORT_INT';
  return formatMeasure(value, formatter);
});


