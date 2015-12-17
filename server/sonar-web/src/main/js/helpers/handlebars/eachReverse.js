module.exports = function (array, options) {
  var ret = '';

  if (array && array.length > 0) {
    for (var i = array.length - 1; i >= 0; i--) {
      ret += options.fn(array[i]);
    }
  } else {
    ret = options.inverse(this);
  }

  return ret;
};
