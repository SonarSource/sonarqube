module.exports = function (value, options) {
  return value != null ? options.fn(this) : options.inverse(this);
};
