import $ from 'jquery';
import _ from 'underscore';
import Backbone from 'backbone';
import ModalForm from 'components/common/modal-form';
import '../templates';

export default ModalForm.extend({
  template: Templates['coding-rules-profile-activation'],

  ui: function () {
    return _.extend(this._super(), {
      qualityProfileSelect: '#coding-rules-quality-profile-activation-select',
      qualityProfileSeverity: '#coding-rules-quality-profile-activation-severity',
      qualityProfileActivate: '#coding-rules-quality-profile-activation-activate',
      qualityProfileParameters: '[name]'
    });
  },

  events: function () {
    return _.extend(this._super(), {
      'click @ui.qualityProfileActivate': 'activate'
    });
  },

  onRender: function () {
    this._super();

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

    this.disableForm();

    return $.ajax({
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

    return _.extend(this._super(), {
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


