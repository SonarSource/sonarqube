module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1),
      options = arguments[arguments.length - 1],
      prefix = args.join(''),
      path = window.location.pathname,
      match = path.indexOf(baseUrl + prefix) === 0;
  return match ? options.fn(this) : options.inverse(this);
};
