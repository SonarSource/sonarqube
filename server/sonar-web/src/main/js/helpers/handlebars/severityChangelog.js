import Handlebars from 'handlebars/runtime';

module.exports = function (severity) {
  var label = '<i class="icon-severity-' + severity.toLowerCase() + '"></i>&nbsp;' + window.t('severity', severity),
      message = window.tp('quality_profiles.severity_set_to_x', label);
  return new Handlebars.default.SafeString(message);
};
