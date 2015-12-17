module.exports = function (componentKey, dashboardKey) {
  var params = [
    { key: 'id', value: componentKey },
    { key: 'did', value: dashboardKey }
  ];

  var matchPeriod = window.location.search.match(/period=(\d+)/);
  if (matchPeriod) {
    // If we have a match for period, check that it is not project-specific
    var period = parseInt(matchPeriod[1], 10);
    if (period <= 3) {
      params.push({ key: 'period', value: period });
    }
  }

  var query = params.map(function (p) {
    return p.key + '=' + encodeURIComponent(p.value);
  }).join('&');
  return baseUrl + '/dashboard/index?' + query;
};
