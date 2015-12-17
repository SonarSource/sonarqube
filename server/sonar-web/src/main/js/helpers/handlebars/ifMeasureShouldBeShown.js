module.exports = function (measure, period, options) {
  if (measure != null || period != null) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
};
