module.exports = function () {
  var args = Array.prototype.slice.call(arguments, 0, -1);
  return window.t.apply(this, args);
};
