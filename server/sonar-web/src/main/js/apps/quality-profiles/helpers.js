Handlebars.registerHelper('profileUrl', function (key) {
  return baseUrl + '/profiles/show?key=' + encodeURIComponent(key);
});

Handlebars.registerHelper('exporterUrl', function (profile, exporterKey) {
  var url = baseUrl + '/api/qualityprofiles/export';
  url += '?language=' + encodeURIComponent(profile.language);
  url += '&name=' + encodeURIComponent(profile.name);
  if (exporterKey != null) {
    url += '&exporterKey=' + encodeURIComponent(exporterKey);
  }
  return url;
});

Handlebars.registerHelper('severityChangelog', function (severity) {
  var label = '<i class="icon-severity-' + severity.toLowerCase() + '"></i>&nbsp;' + t('severity', severity),
      message = tp('quality_profiles.severity_set_to_x', label);
  return new Handlebars.SafeString(message);
});

Handlebars.registerHelper('parameterChangelog', function (value, parameter) {
  if (parameter) {
    return new Handlebars.SafeString(tp('quality_profiles.parameter_set_to_x', value, parameter));
  } else {
    return new Handlebars.SafeString(tp('quality_profiles.changelog.parameter_reset_to_default_value_x', parameter));
  }
});
