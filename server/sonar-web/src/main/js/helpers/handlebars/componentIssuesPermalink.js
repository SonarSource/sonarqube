module.exports = function (componentKey) {
  return baseUrl + '/component_issues/index?id=' + encodeURIComponent(componentKey);
};
