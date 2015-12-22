module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1);
  return args.reduce(function (prev, current) {
    return prev != null ? prev : current;
  }, null);
};
