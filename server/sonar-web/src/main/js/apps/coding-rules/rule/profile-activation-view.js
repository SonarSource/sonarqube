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
define([
  'components/common/modals',
  '../templates'
], function (Modal) {

  var $ = jQuery;

  return Modal.extend({
    template: Templates['coding-rules-profile-activation'],

    ui: {
      qualityProfileSelect: '#coding-rules-quality-profile-activation-select',
      qualityProfileSeverity: '#coding-rules-quality-profile-activation-severity',
      qualityProfileActivate: '#coding-rules-quality-profile-activation-activate',
      qualityProfileParameters: '[name]'
    },

    events: function () {
      return _.extend(Modal.prototype.events.apply(this, arguments), {
        'click @ui.qualityProfileActivate': 'activate'
      });
    },

    onRender: function () {
      Modal.prototype.onRender.apply(this, arguments);

      this.ui.qualityProfileSelect.select2({
        width: '250px',
        minimumResultsForSearch: 5
      });

      var that = this,
          format = function (state) {
            if (!state.id) {
              return state.text;
            } else {
              return '<i class="icon-severity-' + state.id.toLowerCase() + '"></i> ' + state.text;
            }
          },
          severity = (this.model && this.model.get('severity')) || this.options.rule.get('severity');
      this.ui.qualityProfileSeverity.val(severity);
      this.ui.qualityProfileSeverity.select2({
        width: '250px',
        minimumResultsForSearch: 999,
        formatResult: format,
        formatSelection: format
      });
      setTimeout(function () {
        that.$('a').first().focus();
      }, 0);
    },

    activate: function (e) {
      e.preventDefault();
      var that = this,
          profileKey = this.ui.qualityProfileSelect.val(),
          params = this.ui.qualityProfileParameters.map(function () {
            return {
              key: $(this).prop('name'),
              value: $(this).val() || $(this).prop('placeholder') || ''
            };
          }).get(),
          paramsHash = (params.map(function (param) {
            return param.key + '=' + window.csvEscape(param.value);
          })).join(';');

      if (this.model) {
        profileKey = this.model.get('qProfile');
        if (!profileKey) {
          profileKey = this.model.get('key');
        }
      }

      var severity = this.ui.qualityProfileSeverity.val(),
          ruleKey = this.options.rule.get('key');

      this.close();

      return jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/qualityprofiles/activate_rule',
        data: {
          profile_key: profileKey,
          rule_key: ruleKey,
          severity: severity,
          params: paramsHash
        },
        statusCode: {
          // do not show global error
          400: null
        }
      }).done(function () {
        that.trigger('profileActivated', severity, params);
      }).fail(function () {
        that.trigger('profileActivationFailed');
      });
    },

    getAvailableQualityProfiles: function (lang) {
      var activeQualityProfiles = this.collection || new Backbone.Collection(),
          inactiveProfiles = _.reject(this.options.app.qualityProfiles, function (profile) {
            return activeQualityProfiles.findWhere({ key: profile.key });
          });
      return _.filter(inactiveProfiles, function (profile) {
        return profile.lang === lang;
      });
    },

    serializeData: function () {
      var params = this.options.rule.get('params');
      if (this.model != null) {
        var modelParams = this.model.get('params');
        if (_.isArray(modelParams)) {
          params = params.map(function (p) {
            var parentParam = _.findWhere(modelParams, { key: p.key });
            if (parentParam != null) {
              _.extend(p, { value: parentParam.value });
            }
            return p;
          });
        }
      }

      var availableProfiles = this.getAvailableQualityProfiles(this.options.rule.get('lang'));

      return _.extend(Modal.prototype.serializeData.apply(this, arguments), {
        change: this.model && this.model.has('severity'),
        params: params,
        qualityProfiles: availableProfiles,
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
        saveEnabled: !_.isEmpty(availableProfiles) || (this.model && this.model.get('qProfile')),
        isCustomRule: (this.model && this.model.has('templateKey')) || this.options.rule.has('templateKey')
      });
    }
  });

});
