module.exports = function (context, options) {
  var ret = '';
  context.forEach(function (d, i) {
    if (i % 2 === 1) {
      ret += options.fn(d);
    }
  });
  return ret;
};
