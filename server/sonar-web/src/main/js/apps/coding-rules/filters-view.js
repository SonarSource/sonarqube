define([
  'backbone.marionette',
  './rule/manual-rule-creation-view',
  './templates'
], function (Marionette, ManualRuleCreationView) {

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
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
