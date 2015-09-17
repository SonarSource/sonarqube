import $ from 'jquery';
import _ from 'underscore';
import ModalFormView from 'components/common/modal-form';
import '../templates';

export default ModalFormView.extend({
  template: Templates['coding-rules-manual-rule-creation'],

  ui: function () {
    return _.extend(ModalFormView.prototype.ui.apply(this.arguments), {
      manualRuleCreationKey: '#coding-rules-manual-rule-creation-key',
      manualRuleCreationName: '#coding-rules-manual-rule-creation-name',
      manualRuleCreationHtmlDescription: '#coding-rules-manual-rule-creation-html-description',
      manualRuleCreationSeverity: '#coding-rules-manual-rule-creation-severity',
      manualRuleCreationStatus: '#coding-rules-manual-rule-creation-status',
      manualRuleCreationParameters: '[name]',
      manualRuleCreationCreate: '#coding-rules-manual-rule-creation-create',
      manualRuleCreationReactivate: '#coding-rules-manual-rule-creation-reactivate',
      modalFoot: '.modal-foot'
    });
  },

  events: function () {
    return _.extend(ModalFormView.prototype.events.apply(this.arguments), {
      'input @ui.manualRuleCreationName': 'generateKey',
      'keydown @ui.manualRuleCreationName': 'generateKey',
      'keyup @ui.manualRuleCreationName': 'generateKey',

      'input @ui.manualRuleCreationKey': 'flagKey',
      'keydown @ui.manualRuleCreationKey': 'flagKey',
      'keyup @ui.manualRuleCreationKey': 'flagKey',

      'click #coding-rules-manual-rule-creation-cancel': 'hide',
      'click @ui.manualRuleCreationCreate': 'create',
      'click @ui.manualRuleCreationReactivate': 'reactivate'
    });
  },

  onRender: function () {
    ModalFormView.prototype.onRender.apply(this, arguments);
    this.keyModifiedByUser = false;
    this.ui.manualRuleCreationReactivate.addClass('hidden');
  },

  generateKey: function () {
    if (!this.keyModifiedByUser && this.ui.manualRuleCreationKey) {
      var generatedKey = this.ui.manualRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_');
      this.ui.manualRuleCreationKey.val(generatedKey);
    }
  },

  flagKey: function () {
    this.keyModifiedByUser = true;
  },

  create: function () {
    var action = (this.model && this.model.has('key')) ? 'update' : 'create',
        options = {
          name: this.ui.manualRuleCreationName.val(),
          markdown_description: this.ui.manualRuleCreationHtmlDescription.val()
        };
    if (action === 'update') {
      options.key = this.model.get('key');
    } else {
      options.manual_key = this.ui.manualRuleCreationKey.val();
      options.prevent_reactivation = true;
    }
    this.sendRequest(action, options);
  },

  reactivate: function () {
    var options = {
      name: this.existingRule.name,
      markdown_description: this.existingRule.mdDesc,
      manual_key: this.ui.manualRuleCreationKey.val(),
      prevent_reactivation: false
    };
    this.sendRequest('create', options);
  },

  sendRequest: function (action, options) {
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
    }).done(function (r) {
      if (typeof r === 'string') {
        r = JSON.parse(r);
      }
      that.options.app.controller.showDetails(r.rule.key);
      that.destroy();
    }).fail(function (jqXHR) {
      if (jqXHR.status === 409) {
        that.existingRule = jqXHR.responseJSON.rule;
        that.showErrors([], [{ msg: t('coding_rules.reactivate.help') }]);
        that.ui.manualRuleCreationCreate.addClass('hidden');
        that.ui.manualRuleCreationReactivate.removeClass('hidden');
      } else {
        that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
      }
    });
  },

  serializeData: function () {
    return _.extend(ModalFormView.prototype.serializeData.apply(this, arguments), {
      change: this.model && this.model.has('key')
    });
  }
});


