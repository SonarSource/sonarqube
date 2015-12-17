module.exports = function (v1, v2, options) {
  /* eslint eqeqeq: 0 */
  return v1 == v2 ? options.fn(this) : options.inverse(this);
};
