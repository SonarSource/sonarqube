module.exports = function (options) {
  var cond = window.SS && window.SS.lf && window.SS.lf.enableGravatar;
  return cond ? options.fn(this) : options.inverse(this);
};
