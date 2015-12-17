import _ from 'underscore';

module.exports = function (array, len, options) {
  var cond = _.isArray(array) && array.length === +len;
  return cond ? options.fn(this) : options.inverse(this);
};
