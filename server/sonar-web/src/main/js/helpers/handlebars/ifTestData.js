module.exports = function (test, options) {
  if ((test.status !== 'OK') || ((test.status === 'OK') && test.coveredLines)) {
    return options.fn(this);
  } else {
    return options.inverse(this);
  }
};
