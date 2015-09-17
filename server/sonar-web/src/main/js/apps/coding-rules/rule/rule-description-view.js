import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import '../templates';

export default Marionette.ItemView.extend({
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
    return $.ajax({
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


