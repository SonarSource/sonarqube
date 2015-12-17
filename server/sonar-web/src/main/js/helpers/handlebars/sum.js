module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1);
  return args.reduce(function (p, c) {
    return p + +c;
  }, 0);
};
