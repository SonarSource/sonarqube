define([
  'backbone.marionette',
  'templates/coding-rules',
  'coding-rules/rule/custom-rule-view',
  'coding-rules/rule/custom-rule-creation-view'
], function (Marionette, Templates, CustomRuleView, CustomRuleCreationView) {

  return Marionette.CompositeView.extend({
    template: Templates['coding-rules-custom-rules'],
    itemView: CustomRuleView,
    itemViewContainer: '#coding-rules-detail-custom-rules',

    itemViewOptions: function () {
      return {
        app: this.options.app,
        templateRule: this.model
      };
    },

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-create-custom-rule': 'createCustomRule'
    },

    onRender: function () {
      this.$el.toggleClass('hidden', !this.model.get('isTemplate'));
    },

    createCustomRule: function () {
      new CustomRuleCreationView({
        app: this.options.app,
        templateRule: this.model
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
