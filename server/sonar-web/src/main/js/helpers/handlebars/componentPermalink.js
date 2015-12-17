module.exports = function (componentKey) {
  return window.baseUrl + '/dashboard/index?id=' + encodeURIComponent(componentKey);
};
