/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
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
import ModalFormView from '../../../components/common/modal-form';
import Template from '../templates/rule/coding-rules-custom-rule-creation.hbs';
import { csvEscape } from '../../../helpers/csv';
import latinize from '../../../helpers/latinize';
import { translate } from '../../../helpers/l10n';

export default ModalFormView.extend({
  template: Template,

  ui() {
    return {
      ...ModalFormView.prototype.ui.apply(this, arguments),
      customRuleCreationKey: '#coding-rules-custom-rule-creation-key',
      customRuleCreationName: '#coding-rules-custom-rule-creation-name',
      customRuleCreationHtmlDescription: '#coding-rules-custom-rule-creation-html-description',
      customRuleCreationType: '#coding-rules-custom-rule-creation-type',
      customRuleCreationSeverity: '#coding-rules-custom-rule-creation-severity',
      customRuleCreationStatus: '#coding-rules-custom-rule-creation-status',
      customRuleCreationParameters: '[name]',
      customRuleCreationCreate: '#coding-rules-custom-rule-creation-create',
      customRuleCreationReactivate: '#coding-rules-custom-rule-creation-reactivate',
      modalFoot: '.modal-foot'
    };
  },

  events() {
    return {
      ...ModalFormView.prototype.events.apply(this, arguments),
      'input @ui.customRuleCreationName': 'generateKey',
      'keydown @ui.customRuleCreationName': 'generateKey',
      'keyup @ui.customRuleCreationName': 'generateKey',

      'input @ui.customRuleCreationKey': 'flagKey',
      'keydown @ui.customRuleCreationKey': 'flagKey',
      'keyup @ui.customRuleCreationKey': 'flagKey',

      'click #coding-rules-custom-rule-creation-cancel': 'destroy',
      'click @ui.customRuleCreationCreate': 'create',
      'click @ui.customRuleCreationReactivate': 'reactivate'
    };
  },

  generateKey() {
    if (!this.keyModifiedByUser && this.ui.customRuleCreationKey) {
      const generatedKey = latinize(this.ui.customRuleCreationName.val()).replace(
        /[^A-Za-z0-9]/g,
        '_'
      );
      this.ui.customRuleCreationKey.val(generatedKey);
    }
  },

  flagKey() {
    this.keyModifiedByUser = true;
  },

  onRender() {
    ModalFormView.prototype.onRender.apply(this, arguments);

    this.keyModifiedByUser = false;

    const format = function(state) {
      if (!state.id) {
        return state.text;
      } else {
        return `<i class="icon-severity-${state.id.toLowerCase()}"></i> ${state.text}`;
      }
    };
    const type = (this.model && this.model.get('type')) || this.options.templateRule.get('type');
    const severity =
      (this.model && this.model.get('severity')) || this.options.templateRule.get('severity');
    const status =
      (this.model && this.model.get('status')) || this.options.templateRule.get('status');

    this.ui.customRuleCreationType.val(type);
    this.ui.customRuleCreationType.select2({
      width: '250px',
      minimumResultsForSearch: 999
    });

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

  create(e) {
    e.preventDefault();
    const action = this.model && this.model.has('key') ? 'update' : 'create';
    const options = {
      name: this.ui.customRuleCreationName.val(),
      markdown_description: this.ui.customRuleCreationHtmlDescription.val(),
      type: this.ui.customRuleCreationType.val(),
      severity: this.ui.customRuleCreationSeverity.val(),
      status: this.ui.customRuleCreationStatus.val()
    };
    if (this.model && this.model.has('key')) {
      options.key = this.model.get('key');
    } else {
      Object.assign(options, {
        template_key: this.options.templateRule.get('key'),
        custom_key: this.ui.customRuleCreationKey.val(),
        prevent_reactivation: true
      });
    }
    const params = this.ui.customRuleCreationParameters
      .map(function() {
        const node = $(this);
        let value = node.val();
        if (!value && action === 'create') {
          value = node.prop('placeholder') || '';
        }
        return {
          key: node.prop('name'),
          value
        };
      })
      .get();
    options.params = params.map(param => param.key + '=' + csvEscape(param.value)).join(';');
    this.sendRequest(action, options);
  },

  reactivate() {
    const options = {
      name: this.existingRule.name,
      markdown_description: this.existingRule.mdDesc,
      severity: this.existingRule.severity,
      status: this.existingRule.status,
      template_key: this.existingRule.templateKey,
      custom_key: this.ui.customRuleCreationKey.val(),
      prevent_reactivation: false
    };
    const params = this.existingRule.params;
    options.params = params.map(param => param.key + '=' + param.defaultValue).join(';');
    this.sendRequest('create', options);
  },

  sendRequest(action, options) {
    this.$('.alert').addClass('hidden');
    const that = this;
    const url = window.baseUrl + '/api/rules/' + action;
    return $.ajax({
      url,
      type: 'POST',
      data: options,
      statusCode: {
        // do not show global error
        400: null
      }
    })
      .done(() => {
        if (that.options.templateRule) {
          that.options.app.controller.showDetails(that.options.templateRule);
        } else {
          that.options.app.controller.showDetails(that.model);
        }
        that.destroy();
      })
      .fail(jqXHR => {
        if (jqXHR.status === 409) {
          that.existingRule = jqXHR.responseJSON.rule;
          that.showErrors([], [{ msg: translate('coding_rules.reactivate.help') }]);
          that.ui.customRuleCreationCreate.addClass('hidden');
          that.ui.customRuleCreationReactivate.removeClass('hidden');
        } else {
          that.showErrors(jqXHR.responseJSON.errors, jqXHR.responseJSON.warnings);
        }
      });
  },

  serializeData() {
    let params = {};
    if (this.options.templateRule) {
      params = this.options.templateRule.get('params');
    } else if (this.model && this.model.has('params')) {
      params = this.model.get('params').map(p => ({ ...p, value: p.defaultValue }));
    }

    const statuses = ['READY', 'BETA', 'DEPRECATED'].map(status => {
      return {
        id: status,
        text: translate('rules.status', status.toLowerCase())
      };
    });

    return {
      ...ModalFormView.prototype.serializeData.apply(this, arguments),
      params,
      statuses,
      change: this.model && this.model.has('key'),
      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO'],
      types: ['BUG', 'VULNERABILITY', 'CODE_SMELL']
    };
  }
});
