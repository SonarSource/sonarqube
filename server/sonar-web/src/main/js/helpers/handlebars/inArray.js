import _ from 'underscore';

module.exports = function (array, element, options) {
  if (_.isArray(array)) {
    if (array.indexOf(element) !== -1) {
      return options.fn(this);
    } else {
      return options.inverse(this);
    }
  }
};
