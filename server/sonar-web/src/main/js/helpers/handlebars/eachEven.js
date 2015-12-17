module.exports = function (context, options) {
  var ret = '';
  context.forEach(function (d, i) {
    if (i % 2 === 0) {
      ret += options.fn(d);
    }
  });
  return ret;
};
