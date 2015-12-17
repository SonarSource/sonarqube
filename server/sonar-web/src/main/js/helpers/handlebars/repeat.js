module.exports = function (number, options) {
  var ret = '';
  for (var i = 0; i < number; i++) {
    ret += options.fn(this);
  }
  return ret;
};
