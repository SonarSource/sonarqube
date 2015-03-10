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
  'common/modal-form',
  'templates/coding-rules'
], function (ModalFormView) {

  var $ = jQuery;

  return ModalFormView.extend({
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
    },

    generateKey: function () {
      if (!this.keyModifiedByUser && this.ui.manualRuleCreationKey) {
        var generatedKey = this.ui.manualRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_');
        this.ui.manualRuleCreationKey.val(generatedKey);
      }
    },

    flagKey: function () {
      this.keyModifiedByUser = true;
      // Cannot use @ui.manualRuleCreationReactivate.hide() directly since it was not there at initial render
      $(this.ui.manualRuleCreationReactivate.selector).hide();
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
      this.$('.alert').addClass('hidden');
      var that = this,
          url = baseUrl + '/api/rules/' + action;
      return $.post(url, options).done(function (r) {
        if (typeof r === 'string') {
          r = JSON.parse(r);
        }
        that.options.app.controller.showDetails(r.rule.key);
        that.close();
      }).fail(function (jqXHR) {
        if (jqXHR.status === 409) {
          that.existingRule = jqXHR.responseJSON.rule;
          that.$('.alert-warning').removeClass('hidden');
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

});
