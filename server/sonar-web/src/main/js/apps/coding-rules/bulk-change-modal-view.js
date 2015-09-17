import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from 'components/common/modal-form';
import './templates';

export default ModalFormView.extend({
  template: Templates['coding-rules-bulk-change-modal'],

  ui: function () {
    return _.extend(ModalFormView.prototype.ui.apply(this, arguments), {
      codingRulesSubmitBulkChange: '#coding-rules-submit-bulk-change'
    });
  },

  showSuccessMessage: function (profile, succeeded) {
    var profileBase = _.findWhere(this.options.app.qualityProfiles, { key: profile }),
        profileName = profileBase != null ? profileBase.name : profile,
        message = tp('coding_rules.bulk_change.success', profileName, profileBase.language, succeeded);
    this.ui.messagesContainer.append('<div class="alert alert-success">' + message + '</div>');
  },

  showWarnMessage: function (profile, succeeded, failed) {
    var profileBase = _.findWhere(this.options.app.qualityProfiles, { key: profile }),
        profileName = profileBase != null ? profileBase.name : profile,
        message = tp('coding_rules.bulk_change.warning', profileName, profileBase.language, succeeded, failed);
    this.ui.messagesContainer.append('<div class="alert alert-warning">' + message + '</div>');
  },

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.$('#coding-rules-bulk-change-profile').select2({
      width: '250px',
      minimumResultsForSearch: 1,
      openOnEnter: false
    });
  },

  onFormSubmit: function () {
    ModalFormView.prototype.onFormSubmit.apply(this, arguments);
    var url = baseUrl + '/api/qualityprofiles/' + this.options.action + '_rules',
        options = _.extend({}, this.options.app.state.get('query'), { wsAction: this.options.action }),
        profiles = this.$('#coding-rules-bulk-change-profile').val() || [this.options.param];
    this.ui.messagesContainer.empty();
    this.sendRequests(url, options, profiles);
  },

  sendRequests: function (url, options, profiles) {
    var that = this,
        looper = $.Deferred().resolve();
    profiles.forEach(function (profile) {
      var opts = _.extend({}, options, { profile_key: profile });
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

  getAvailableQualityProfiles: function () {
    var queryLanguages = this.options.app.state.get('query').languages,
        languages = queryLanguages && queryLanguages.length > 0 ? queryLanguages.split(',') : [],
        profiles = this.options.app.qualityProfiles;
    if (languages.length > 0) {
      profiles = _.filter(profiles, function (profile) {
        return languages.indexOf(profile.lang) !== -1;
      });
    }
    return profiles;
  },

  serializeData: function () {
    var profile = _.findWhere(this.options.app.qualityProfiles, { key: this.options.param });
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


