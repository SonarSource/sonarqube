module.exports = function () {
  var args = Array.prototype.slice.call(arguments),
      ret = null;
  args.forEach(function (arg) {
    if (typeof arg === 'string' && ret == null) {
      ret = arg;
    }
  });
  return ret || '';
};
