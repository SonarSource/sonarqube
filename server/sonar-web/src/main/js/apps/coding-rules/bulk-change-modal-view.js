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
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/coding-rules-bulk-change-modal.hbs';
import { translateWithParameters } from '../../helpers/l10n';

export default ModalFormView.extend({
  template: Template,

  ui () {
    return _.extend(ModalFormView.prototype.ui.apply(this, arguments), {
      codingRulesSubmitBulkChange: '#coding-rules-submit-bulk-change'
    });
  },

  showSuccessMessage (profile, succeeded) {
    const profileBase = _.findWhere(this.options.app.qualityProfiles, { key: profile });
    const profileName = profileBase != null ? profileBase.name : profile;
    const message = translateWithParameters('coding_rules.bulk_change.success',
        profileName, profileBase.language, succeeded);
    this.ui.messagesContainer.append(`<div class="alert alert-success">${message}</div>`);
  },

  showWarnMessage (profile, succeeded, failed) {
    const profileBase = _.findWhere(this.options.app.qualityProfiles, { key: profile });
    const profileName = profileBase != null ? profileBase.name : profile;
    const message = translateWithParameters('coding_rules.bulk_change.warning',
        profileName, profileBase.language, succeeded, failed);
    this.ui.messagesContainer.append(`<div class="alert alert-warning">${message}</div>`);
  },

  onRender () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('#coding-rules-bulk-change-profile').select2({
      width: '250px',
      minimumResultsForSearch: 1,
      openOnEnter: false
    });
  },

  onFormSubmit () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    const url = `${window.baseUrl}/api/qualityprofiles/${this.options.action}_rules`;
    const options = _.extend({}, this.options.app.state.get('query'), { wsAction: this.options.action });
    const profiles = this.$('#coding-rules-bulk-change-profile').val() || [this.options.param];
    this.ui.messagesContainer.empty();
    this.sendRequests(url, options, profiles);
  },

  sendRequests (url, options, profiles) {
    const that = this;
    let looper = $.Deferred().resolve();
    profiles.forEach(function (profile) {
      const opts = _.extend({}, options, { profile_key: profile });
      looper = looper.then(function () {
        return $.post(url, opts).done(function (r) {
          if (r.failed) {
            that.showWarnMessage(profile, r.succeeded, r.failed);
          } else {
            that.showSuccessMessage(profile, r.succeeded);
          }
        });
      });
    });
    looper.done(function () {
      that.options.app.controller.fetchList();
      that.$(that.ui.codingRulesSubmitBulkChange.selector).hide();
      that.$('.modal-field').hide();
      that.$('.js-modal-close').focus();
    });
  },

  getAvailableQualityProfiles () {
    const queryLanguages = this.options.app.state.get('query').languages;
    const languages = queryLanguages && queryLanguages.length > 0 ? queryLanguages.split(',') : [];
    let profiles = this.options.app.qualityProfiles;
    if (languages.length > 0) {
      profiles = _.filter(profiles, function (profile) {
        return languages.indexOf(profile.lang) !== -1;
      });
    }
    return profiles;
  },

  serializeData () {
    const profile = _.findWhere(this.options.app.qualityProfiles, { key: this.options.param });
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      action: this.options.action,
      state: this.options.app.state.toJSON(),
      qualityProfile: this.options.param,
      qualityProfileName: profile != null ? profile.name : null,
      qualityProfiles: this.options.app.qualityProfiles,
      availableQualityProfiles: this.getAvailableQualityProfiles()
    });
  }
});
