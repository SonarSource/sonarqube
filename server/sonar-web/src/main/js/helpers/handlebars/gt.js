module.exports = function (v1, v2, options) {
  return v1 > v2 ? options.fn(this) : options.inverse(this);
};
