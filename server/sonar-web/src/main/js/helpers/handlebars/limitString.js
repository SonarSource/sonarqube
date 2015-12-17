module.exports = function (str) {
  if (typeof str === 'string') {
    var LIMIT = 30;
    return str.length > LIMIT ? str.substr(0, LIMIT) + '...' : str;
  }
};
