/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import ModalForm from '../../../components/common/modal-form';
import Template from '../templates/rule/coding-rules-profile-activation.hbs';
import { csvEscape } from '../../../helpers/csv';

export default ModalForm.extend({
  template: Template,

  ui: function () {
    return _.extend(ModalForm.prototype.ui.apply(this, arguments), {
      qualityProfileSelect: '#coding-rules-quality-profile-activation-select',
      qualityProfileSeverity: '#coding-rules-quality-profile-activation-severity',
      qualityProfileActivate: '#coding-rules-quality-profile-activation-activate',
      qualityProfileParameters: '[name]'
    });
  },

  events: function () {
    return _.extend(ModalForm.prototype.events.apply(this, arguments), {
      'click @ui.qualityProfileActivate': 'activate'
    });
  },

  onRender: function () {
    ModalForm.prototype.onRender.apply(this, arguments);

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
          return param.key + '=' + csvEscape(param.value);
        })).join(';');

    if (this.model) {
      profileKey = this.model.get('qProfile');
      if (!profileKey) {
        profileKey = this.model.get('key');
      }
    }

    var severity = this.ui.qualityProfileSeverity.val(),
        ruleKey = this.options.rule.get('key');

    this.disableForm();

    return $.ajax({
      type: 'POST',
      url: '/api/qualityprofiles/activate_rule',
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
      that.destroy();
      that.trigger('profileActivated', severity, params, profileKey);
    }).fail(function (jqXHR) {
      that.enableForm();
      that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
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

    var availableProfiles = this.getAvailableQualityProfiles(this.options.rule.get('lang')),
        contextProfile = this.options.app.state.get('query').qprofile;

    return _.extend(ModalForm.prototype.serializeData.apply(this, arguments), {
      change: this.model && this.model.has('severity'),
      params: params,
      qualityProfiles: availableProfiles,
      contextProfile: contextProfile,
      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
      saveEnabled: !_.isEmpty(availableProfiles) || (this.model && this.model.get('qProfile')),
      isCustomRule: (this.model && this.model.has('templateKey')) || this.options.rule.has('templateKey')
    });
  }
});
