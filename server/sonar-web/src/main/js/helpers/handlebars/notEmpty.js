import _ from 'underscore';

module.exports = function (array, options) {
  var cond = _.isArray(array) && array.length > 0;
  return cond ? options.fn(this) : options.inverse(this);
};
