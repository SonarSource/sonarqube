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
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-rule-description.hbs';
import confirmDialog from '../confirm-dialog';
import { translate } from '../../../helpers/l10n';

export default Marionette.ItemView.extend({
  template: Template,

  modelEvents: {
    change: 'render'
  },

  ui: {
    descriptionExtra: '#coding-rules-detail-description-extra',
    extendDescriptionLink: '#coding-rules-detail-extend-description',
    extendDescriptionForm: '.coding-rules-detail-extend-description-form',
    extendDescriptionSubmit: '#coding-rules-detail-extend-description-submit',
    extendDescriptionRemove: '#coding-rules-detail-extend-description-remove',
    extendDescriptionText: '#coding-rules-detail-extend-description-text',
    cancelExtendDescription: '#coding-rules-detail-extend-description-cancel'
  },

  events: {
    'click @ui.extendDescriptionLink': 'showExtendDescriptionForm',
    'click @ui.cancelExtendDescription': 'hideExtendDescriptionForm',
    'click @ui.extendDescriptionSubmit': 'submitExtendDescription',
    'click @ui.extendDescriptionRemove': 'removeExtendedDescription'
  },

  showExtendDescriptionForm() {
    this.ui.descriptionExtra.addClass('hidden');
    this.ui.extendDescriptionForm.removeClass('hidden');
    this.ui.extendDescriptionText.focus();
  },

  hideExtendDescriptionForm() {
    this.ui.descriptionExtra.removeClass('hidden');
    this.ui.extendDescriptionForm.addClass('hidden');
  },

  submitExtendDescription() {
    const that = this;
    this.ui.extendDescriptionForm.addClass('hidden');
    const data = {
      key: this.model.get('key'),
      markdown_note: this.ui.extendDescriptionText.val()
    };
    if (this.options.app.organization) {
      data.organization = this.options.app.organization;
    }
    return $.ajax({
      type: 'POST',
      url: window.baseUrl + '/api/rules/update',
      dataType: 'json',
      data
    })
      .done(r => {
        that.model.set({
          htmlNote: r.rule.htmlNote,
          mdNote: r.rule.mdNote
        });
        that.render();
      })
      .fail(() => {
        that.render();
      });
  },

  removeExtendedDescription() {
    const that = this;
    confirmDialog({
      html: translate('coding_rules.remove_extended_description.confirm'),
      yesHandler() {
        that.ui.extendDescriptionText.val('');
        that.submitExtendDescription();
      }
    });
  },

  serializeData() {
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      isCustom: this.model.get('isCustom'),
      canCustomizeRule: this.options.app.canWrite
    };
  }
});
