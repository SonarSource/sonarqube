/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
(function () {

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

})();
