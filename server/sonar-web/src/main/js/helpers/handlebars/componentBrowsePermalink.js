module.exports = function (componentKey) {
  return baseUrl + '/components/index?id=' + encodeURIComponent(componentKey);
};
