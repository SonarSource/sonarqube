module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1),
      options = arguments[arguments.length - 1],
      any = args.reduce(function (prev, current) {
        return prev || current;
      }, false);
  return any ? options.fn(this) : options.inverse(this);
};
