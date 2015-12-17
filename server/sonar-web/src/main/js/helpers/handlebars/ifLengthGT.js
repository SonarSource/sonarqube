import _ from 'underscore';

module.exports = function (array, len, options) {
  return _.size(array) > len ? options.fn(this) : options.inverse(this);
};
