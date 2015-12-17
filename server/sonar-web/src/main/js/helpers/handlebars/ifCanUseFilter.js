module.exports = function (query, options) {
  var cond = window.SS.user || query.indexOf('__me__') === -1;
  return cond ? options.fn(this) : options.inverse(this);
};
