module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1),
      options = arguments[arguments.length - 1],
      notEmpty = args.reduce(function (prev, current) {
        return prev || (current && current.length > 0);
      }, false);
  return notEmpty ? options.fn(this) : options.inverse(this);
};
