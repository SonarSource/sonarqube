define([
  'templates/coding-rules'
], function () {

  var $ = jQuery;

  return Marionette.ItemView.extend({
    tagName: 'tr',
    template: Templates['coding-rules-custom-rule'],

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-delete-custom-rule': 'deleteRule'
    },

    deleteRule: function () {
      var that = this;
      window.confirmDialog({
        title: t('delete'),
        html: t('are_you_sure'),
        yesHandler: function () {
          var p = window.process.addBackgroundProcess(),
              url = baseUrl + '/api/rules/delete',
              options = { key: that.model.id };
          $.post(url, options).done(function () {
            that.model.collection.remove(that.model);
            that.close();
            window.process.finishBackgroundProcess(p);
          }).fail(function () {
            window.process.failBackgroundProcess(p);
          });
        }
      });
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite,
        templateRule: this.options.templateRule,
        permalink: baseUrl + '/coding_rules/show?key=' + encodeURIComponent(this.model.id)
      });
    }
  });

});
