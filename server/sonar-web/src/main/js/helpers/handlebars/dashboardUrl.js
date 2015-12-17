module.exports = function (componentKey, componentQualifier) {
  var url = baseUrl + '/dashboard/index?id=' + encodeURIComponent(componentKey);
  if (componentQualifier === 'FIL' || componentQualifier === 'CLA') {
    url += '&metric=sqale_index';
  }
  return url;
};
