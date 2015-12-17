module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1),
      options = arguments[arguments.length - 1],
      all = args.reduce(function (prev, current) {
        return prev && current;
      }, true);
  return all ? options.fn(this) : options.inverse(this);
};
