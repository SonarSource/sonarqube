import { formatMeasure } from '../measures';

module.exports = function (value, mode) {
  var formatter = mode === 'debt' ? 'SHORT_WORK_DUR' : 'SHORT_INT';
  return formatMeasure(value, formatter);
};
