import _ from 'underscore';

function getQuery (query, separator) {
  separator = separator || '|';
  var route = [];
  _.forEach(query, function (value, property) {
    route.push('' + property + '=' + encodeURIComponent(value));
  });
  return route.join(separator);
}

module.exports = function (query, mode) {
  var r = _.extend({}, query);
  if (mode === 'debt') {
    r.facetMode = 'debt';
  }
  if (r.componentKey != null) {
    return baseUrl + '/component_issues/index?id=' + encodeURIComponent(r.componentKey) +
        '#' + getQuery(_.omit(r, 'componentKey'));
  } else {
    return baseUrl + '/issues/search#' + getQuery(r);
  }
};
