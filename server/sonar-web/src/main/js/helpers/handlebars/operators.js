module.exports = function (options) {
  var ops = ['LT', 'GT', 'EQ', 'NE'];

  return ops.reduce(function (prev, current) {
    return prev + options.fn(current);
  }, '');
};
