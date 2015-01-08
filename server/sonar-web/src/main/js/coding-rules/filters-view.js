define([
    'coding-rules/rule/manual-rule-creation-view',
    'templates/coding-rules'
], function (ManualRuleCreationView) {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-filters'],

    events: {
      'click .js-new-search': 'newSearch',
      'click .js-create-manual-rule': 'createManualRule'
    },

    newSearch: function () {
      this.options.app.controller.newSearch();
    },

    createManualRule: function() {
      new ManualRuleCreationView({
        app: this.options.app
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
