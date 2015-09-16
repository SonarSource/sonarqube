define([
  './rule/manual-rule-creation-view',
  './templates'
], function (ManualRuleCreationView) {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-filters'],

    events: {
      'click .js-create-manual-rule': 'createManualRule'
    },

    createManualRule: function () {
      new ManualRuleCreationView({
        app: this.options.app
      }).render();
    },

    serializeData: function () {
      return _.extend(this._super(), { canWrite: this.options.app.canWrite });
    }
  });

});
