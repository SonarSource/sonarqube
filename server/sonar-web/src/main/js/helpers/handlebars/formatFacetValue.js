import { formatMeasure } from '../measures';

module.exports = function (value, facetMode) {
  var formatter = facetMode === 'debt' ? 'SHORT_WORK_DUR' : 'SHORT_INT';
  return formatMeasure(value, formatter);
};


