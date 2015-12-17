module.exports = function (context, property, options) {
  var ret = '';
  context.forEach(function (d, i) {
    var changed = i > 0 ? d[property] !== context[i - 1][property] : true,
        c = _.extend({ changed: changed }, d);
    ret += options.fn(c);
  });
  return ret;
};
