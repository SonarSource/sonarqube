import _ from 'underscore';

module.exports = function (context, options) {
  var ret = '';
  context.forEach(function (d, i) {
    var c = _.extend({ index: i }, d);
    ret += options.fn(c);
  });
  return ret;
};
