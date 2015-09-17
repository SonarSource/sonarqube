import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from 'components/common/modal-form';
import '../templates';

export default ModalFormView.extend({
  template: Templates['coding-rules-custom-rule-creation'],

  ui: function () {
    return _.extend(ModalFormView.prototype.ui.apply(this, arguments), {
      customRuleCreationKey: '#coding-rules-custom-rule-creation-key',
      customRuleCreationName: '#coding-rules-custom-rule-creation-name',
      customRuleCreationHtmlDescription: '#coding-rules-custom-rule-creation-html-description',
      customRuleCreationSeverity: '#coding-rules-custom-rule-creation-severity',
      customRuleCreationStatus: '#coding-rules-custom-rule-creation-status',
      customRuleCreationParameters: '[name]',
      customRuleCreationCreate: '#coding-rules-custom-rule-creation-create',
      customRuleCreationReactivate: '#coding-rules-custom-rule-creation-reactivate',
      modalFoot: '.modal-foot'
    });
  },

  events: function () {
    return _.extend(ModalFormView.prototype.events.apply(this, arguments), {
      'input @ui.customRuleCreationName': 'generateKey',
      'keydown @ui.customRuleCreationName': 'generateKey',
      'keyup @ui.customRuleCreationName': 'generateKey',

      'input @ui.customRuleCreationKey': 'flagKey',
      'keydown @ui.customRuleCreationKey': 'flagKey',
      'keyup @ui.customRuleCreationKey': 'flagKey',

      'click #coding-rules-custom-rule-creation-cancel': 'destroy',
      'click @ui.customRuleCreationCreate': 'create',
      'click @ui.customRuleCreationReactivate': 'reactivate'
    });
  },

  generateKey: function () {
    if (!this.keyModifiedByUser && this.ui.customRuleCreationKey) {
      var generatedKey = this.ui.customRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_');
      this.ui.customRuleCreationKey.val(generatedKey);
    }
  },

  flagKey: function () {
    this.keyModifiedByUser = true;
  },

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);

    this.keyModifiedByUser = false;

    var format = function (state) {
          if (!state.id) {
            return state.text;
          } else {
            return '<i class="icon-severity-' + state.id.toLowerCase() + '"></i> ' + state.text;
          }
        },
        severity = (this.model && this.model.get('severity')) || this.options.templateRule.get('severity'),
        status = (this.model && this.model.get('status')) || this.options.templateRule.get('status');

    this.ui.customRuleCreationSeverity.val(severity);
    this.ui.customRuleCreationSeverity.select2({
      width: '250px',
      minimumResultsForSearch: 999,
      formatResult: format,
      formatSelection: format
    });

    this.ui.customRuleCreationStatus.val(status);
    this.ui.customRuleCreationStatus.select2({
      width: '250px',
      minimumResultsForSearch: 999
    });
  },

  create: function (e) {
    e.preventDefault();
    var action = (this.model && this.model.has('key')) ? 'update' : 'create',
        options = {
          name: this.ui.customRuleCreationName.val(),
          markdown_description: this.ui.customRuleCreationHtmlDescription.val(),
          severity: this.ui.customRuleCreationSeverity.val(),
          status: this.ui.customRuleCreationStatus.val()
        };
    if (this.model && this.model.has('key')) {
      options.key = this.model.get('key');
    } else {
      _.extend(options, {
        template_key: this.options.templateRule.get('key'),
        custom_key: this.ui.customRuleCreationKey.val(),
        prevent_reactivation: true
      });
    }
    var params = this.ui.customRuleCreationParameters.map(function () {
      var node = $(this),
          value = node.val();
      if (!value && action === 'create') {
        value = node.prop('placeholder') || '';
      }
      return {
        key: node.prop('name'),
        value: value
      };
    }).get();
    options.params = params.map(function (param) {
      return param.key + '=' + window.csvEscape(param.value);
    }).join(';');
    this.sendRequest(action, options);
  },

  reactivate: function () {
    var options = {
          name: this.existingRule.name,
          markdown_description: this.existingRule.mdDesc,
          severity: this.existingRule.severity,
          status: this.existingRule.status,
          template_key: this.existingRule.templateKey,
          custom_key: this.ui.customRuleCreationKey.val(),
          prevent_reactivation: false
        },
        params = this.existingRule.params;
    options.params = params.map(function (param) {
      return param.key + '=' + param.defaultValue;
    }).join(';');
    this.sendRequest('create', options);
  },

  sendRequest: function (action, options) {
    this.$('.alert').addClass('hidden');
    var that = this,
        url = baseUrl + '/api/rules/' + action;
    return $.ajax({
      url: url,
      type: 'POST',
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    }).done(function () {
      if (that.options.templateRule) {
        that.options.app.controller.showDetails(that.options.templateRule);
      } else {
        that.options.app.controller.showDetails(that.model);
      }
      that.destroy();
    }).fail(function (jqXHR) {
      if (jqXHR.status === 409) {
        that.existingRule = jqXHR.responseJSON.rule;
        that.showErrors([], [{ msg: t('coding_rules.reactivate.help') }]);
        that.ui.customRuleCreationCreate.addClass('hidden');
        that.ui.customRuleCreationReactivate.removeClass('hidden');
      } else {
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      }
    });
  },

  serializeData: function () {
    var params = {};
    if (this.options.templateRule) {
      params = this.options.templateRule.get('params');
    } else if (this.model && this.model.has('params')) {
      params = this.model.get('params').map(function (p) {
        return _.extend(p, { value: p.defaultValue });
      });
    }

    var statuses = ['READY', 'BETA', 'DEPRECATED'].map(function (status) {
      return {
        id: status,
        text: t('rules.status', status.toLowerCase())
      };
    });

    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      change: this.model && this.model.has('key'),
      params: params,
      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
      statuses: statuses
    });
  }
});


