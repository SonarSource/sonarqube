module.exports = function (array, separator, options) {
  var ret = '';

  if (array && array.length > 0) {
    for (var i = 0, n = array.length; i < n; i++) {
      ret += options.fn(array[i]);
      if (i < n - 1) {
        ret += separator;
      }
    }
  } else {
    ret = options.inverse(this);
  }

  return ret;
};
