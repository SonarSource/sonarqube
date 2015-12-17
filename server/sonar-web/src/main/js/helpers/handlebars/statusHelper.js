import Handlebars from 'handlebars/runtime';

module.exports = function (status, resolution) {
  var s = '<i class="icon-status-' + status.toLowerCase() + '"></i>&nbsp;' + window.t('issue.status', status);
  if (resolution != null) {
    s = s + '&nbsp;(' + window.t('issue.resolution', resolution) + ')';
  }
  return new Handlebars.default.SafeString(s);
};
