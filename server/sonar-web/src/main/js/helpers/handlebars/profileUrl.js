module.exports = function (key) {
  return baseUrl + '/profiles/show?key=' + encodeURIComponent(key);
};
