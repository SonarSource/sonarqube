module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1);
  return window.translate.apply(this, args);
};
