module.exports = function () {
  /* eslint no-console: 0 */
  var args = Array.prototype.slice.call(arguments, 0, -1);
  console.log.apply(console, args);
};
