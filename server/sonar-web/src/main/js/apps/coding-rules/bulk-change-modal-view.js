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
import ModalFormView from '../../components/common/modal-form';
import Template from './templates/coding-rules-bulk-change-modal.hbs';
import { translateWithParameters } from '../../helpers/l10n';

export default ModalFormView.extend({
  template: Template,

  ui() {
    return {
      ...ModalFormView.prototype.ui.apply(this, arguments),
      codingRulesSubmitBulkChange: '#coding-rules-submit-bulk-change'
    };
  },

  showSuccessMessage(profile, succeeded) {
    const profileBase = this.options.app.qualityProfiles.find(p => p.key === profile);
    const message = translateWithParameters(
      'coding_rules.bulk_change.success',
      profileBase.name,
      profileBase.language,
      succeeded
    );
    this.ui.messagesContainer.append(`<div class="alert alert-success">${message}</div>`);
  },

  showWarnMessage(profile, succeeded, failed) {
    const profileBase = this.options.app.qualityProfiles.find(p => p.key === profile);
    const message = translateWithParameters(
      'coding_rules.bulk_change.warning',
      profileBase.name,
      profileBase.language,
      succeeded,
      failed
    );
    this.ui.messagesContainer.append(`<div class="alert alert-warning">${message}</div>`);
  },

  onRender() {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('#coding-rules-bulk-change-profile').select2({
      width: '250px',
      minimumResultsForSearch: 1,
      openOnEnter: false
    });
  },

  onFormSubmit() {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    const url = `${window.baseUrl}/api/qualityprofiles/${this.options.action}_rules`;
    const options = { ...this.options.app.state.get('query'), wsAction: this.options.action };
    const profiles = this.$('#coding-rules-bulk-change-profile').val() || [this.options.param];
    this.ui.messagesContainer.empty();
    this.sendRequests(url, options, profiles);
  },

  sendRequests(url, options, profiles) {
    const that = this;
    let looper = $.Deferred().resolve();
    this.disableForm();
    profiles.forEach(profile => {
      const opts = { ...options, profile_key: profile };
      looper = looper.then(() => {
        return $.post(url, opts).done(r => {
          if (!that.isDestroyed) {
            if (r.failed) {
              that.showWarnMessage(profile, r.succeeded, r.failed);
            } else {
              that.showSuccessMessage(profile, r.succeeded);
            }
          }
        });
      });
    });
    looper.done(() => {
      that.options.app.controller.fetchList();
      if (!that.isDestroyed) {
        that.$(that.ui.codingRulesSubmitBulkChange.selector).hide();
        that.enableForm();
        that.$('.modal-field').hide();
        that.$('.js-modal-close').focus();
      }
    });
  },

  getAvailableQualityProfiles() {
    const queryLanguages = this.options.app.state.get('query').languages;
    const languages = queryLanguages && queryLanguages.length > 0 ? queryLanguages.split(',') : [];
    let profiles = this.options.app.qualityProfiles;
    if (languages.length > 0) {
      profiles = profiles.filter(profile => languages.indexOf(profile.lang) !== -1);
    }
    return profiles.filter(profile => !profile.isBuiltIn);
  },

  serializeData() {
    const profile = this.options.app.qualityProfiles.find(p => p.key === this.options.param);
    return {
      ...ModalFormView.prototype.serializeData.apply(this, arguments),
      action: this.options.action,
      state: this.options.app.state.toJSON(),
      qualityProfile: this.options.param,
      qualityProfileName: profile != null ? profile.name : null,
      qualityProfiles: this.options.app.qualityProfiles,
      availableQualityProfiles: this.getAvailableQualityProfiles()
    };
  }
});
