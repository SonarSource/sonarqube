import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import Template from '../templates/rule/coding-rules-custom-rule.hbs';
import confirmDialog from '../confirm-dialog';

export default Marionette.ItemView.extend({
  tagName: 'tr',
  template: Template,

  modelEvents: {
    'change': 'render'
  },

  events: {
    'click .js-delete-custom-rule': 'deleteRule'
  },

  deleteRule: function () {
    var that = this;
    confirmDialog({
      title: window.t('delete'),
      html: window.t('are_you_sure'),
      yesHandler: function () {
        var url = baseUrl + '/api/rules/delete',
            options = { key: that.model.id };
        $.post(url, options).done(function () {
          that.model.collection.remove(that.model);
          that.destroy();
        });
      }
    });
  },

  serializeData: function () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      canWrite: this.options.app.canWrite,
      templateRule: this.options.templateRule,
      permalink: baseUrl + '/coding_rules/#rule_key=' + encodeURIComponent(this.model.id)
    });
  }
});
