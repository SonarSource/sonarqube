/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import Backbone from 'backbone';
import ModalForm from '../../../components/common/modal-form';
import Template from '../templates/rule/coding-rules-profile-activation.hbs';
import { csvEscape } from '../../../helpers/csv';
import { sortProfiles } from '../../quality-profiles/utils';

export default ModalForm.extend({
  template: Template,

  ui() {
    return {
      ...ModalForm.prototype.ui.apply(this, arguments),
      qualityProfileSelect: '#coding-rules-quality-profile-activation-select',
      qualityProfileSeverity: '#coding-rules-quality-profile-activation-severity',
      qualityProfileActivate: '#coding-rules-quality-profile-activation-activate',
      qualityProfileParameters: '[name]'
    };
  },

  events() {
    return {
      ...ModalForm.prototype.events.apply(this, arguments),
      'click @ui.qualityProfileActivate': 'activate'
    };
  },

  onRender() {
    ModalForm.prototype.onRender.apply(this, arguments);

    this.ui.qualityProfileSelect.select2({
      width: '250px',
      minimumResultsForSearch: 5
    });

    const that = this;
    const format = function(state) {
      if (!state.id) {
        return state.text;
      } else {
        return `<i class="icon-severity-${state.id.toLowerCase()}"></i> ${state.text}`;
      }
    };
    const severity =
      (this.model && this.model.get('severity')) || this.options.rule.get('severity');
    this.ui.qualityProfileSeverity.val(severity);
    this.ui.qualityProfileSeverity.select2({
      width: '250px',
      minimumResultsForSearch: 999,
      formatResult: format,
      formatSelection: format
    });
    setTimeout(() => {
      that.$('a').first().focus();
    }, 0);
  },

  activate(e) {
    e.preventDefault();
    const that = this;
    let profileKey = this.ui.qualityProfileSelect.val();
    const params = this.ui.qualityProfileParameters
      .map(function() {
        return {
          key: $(this).prop('name'),
          value: $(this).val() || $(this).prop('placeholder') || ''
        };
      })
      .get();
    const paramsHash = params.map(param => param.key + '=' + csvEscape(param.value)).join(';');

    if (this.model) {
      profileKey = this.model.get('qProfile');
      if (!profileKey) {
        profileKey = this.model.get('key');
      }
    }

    const severity = this.ui.qualityProfileSeverity.val();
    const ruleKey = this.options.rule.get('key');

    this.disableForm();

    return $.ajax({
      type: 'POST',
      url: window.baseUrl + '/api/qualityprofiles/activate_rule',
      data: {
        severity,
        profile_key: profileKey,
        rule_key: ruleKey,
        params: paramsHash
      },
      statusCode: {
        // do not show global error
        400: null
      }
    })
      .done(() => {
        that.destroy();
        that.trigger('profileActivated', severity, params, profileKey);
      })
      .fail(jqXHR => {
        that.enableForm();
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      });
  },

  getAvailableQualityProfiles(lang) {
    const activeQualityProfiles = this.collection || new Backbone.Collection();
    const inactiveProfiles = this.options.app.qualityProfiles.filter(
      profile => !activeQualityProfiles.findWhere({ key: profile.key })
    );
    return inactiveProfiles
      .filter(profile => profile.lang === lang)
      .filter(profile => !profile.isBuiltIn);
  },

  serializeData() {
    let params = this.options.rule.get('params');
    if (this.model != null) {
      const modelParams = this.model.get('params');
      if (Array.isArray(modelParams)) {
        params = params.map(p => {
          const parentParam = modelParams.find(param => param.key === p.key);
          if (parentParam != null) {
            Object.assign(p, { value: parentParam.value });
          }
          return p;
        });
      }
    }

    const availableProfiles = this.getAvailableQualityProfiles(this.options.rule.get('lang'));
    const contextProfile = this.options.app.state.get('query').qprofile;

    // decrease depth by 1, so the top level starts at 0
    const profilesWithDepth = sortProfiles(availableProfiles).map(profile => ({
      ...profile,
      depth: profile.depth - 1
    }));

    return {
      ...ModalForm.prototype.serializeData.apply(this, arguments),
      params,
      contextProfile,
      change: this.model && this.model.has('severity'),
      qualityProfiles: profilesWithDepth,
      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
      saveEnabled: availableProfiles.length > 0 || (this.model && this.model.get('qProfile')),
      isCustomRule:
        (this.model && this.model.has('templateKey')) || this.options.rule.has('templateKey')
    };
  }
});
