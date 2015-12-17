module.exports = function () {
  var url = Array.prototype.slice.call(arguments, 0, -1).join('');
  return window.baseUrl + url;
};
