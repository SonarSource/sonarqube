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
  '../templates'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-rule-description'],

    modelEvents: {
      'change': 'render'
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

    showExtendDescriptionForm: function () {
      this.ui.descriptionExtra.addClass('hidden');
      this.ui.extendDescriptionForm.removeClass('hidden');
      this.ui.extendDescriptionText.focus();
    },

    hideExtendDescriptionForm: function () {
      this.ui.descriptionExtra.removeClass('hidden');
      this.ui.extendDescriptionForm.addClass('hidden');
    },

    submitExtendDescription: function () {
      var that = this;
      this.ui.extendDescriptionForm.addClass('hidden');
      return jQuery.ajax({
        type: 'POST',
        url: baseUrl + '/api/rules/update',
        dataType: 'json',
        data: {
          key: this.model.get('key'),
          markdown_note: this.ui.extendDescriptionText.val()
        }
      }).done(function (r) {
        that.model.set({
          htmlNote: r.rule.htmlNote,
          mdNote: r.rule.mdNote
        });
        that.render();
      }).fail(function () {
        that.render();
      });
    },

    removeExtendedDescription: function () {
      var that = this;
      window.confirmDialog({
        html: t('coding_rules.remove_extended_description.confirm'),
        yesHandler: function () {
          that.ui.extendDescriptionText.val('');
          that.submitExtendDescription();
        }
      });
    },

    serializeData: function () {
      var isEditable = this.options.app.canWrite && (this.model.get('isManual') || this.model.get('isCustom'));

      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        isEditable: isEditable,
        canWrite: this.options.app.canWrite
      });
    }
  });

});
