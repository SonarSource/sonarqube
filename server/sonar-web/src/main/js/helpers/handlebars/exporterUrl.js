module.exports = function (profile, exporterKey) {
  var url = baseUrl + '/api/qualityprofiles/export';
  url += '?language=' + encodeURIComponent(profile.language);
  url += '&name=' + encodeURIComponent(profile.name);
  if (exporterKey != null) {
    url += '&exporterKey=' + encodeURIComponent(exporterKey);
  }
  return url;
};
