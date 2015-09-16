define([
  './custom-rule-view',
  './custom-rule-creation-view',
  '../templates'
], function (CustomRuleView, CustomRuleCreationView) {

  return Marionette.CompositeView.extend({
    template: Templates['coding-rules-custom-rules'],
    childView: CustomRuleView,
    childViewContainer: '#coding-rules-detail-custom-rules',

    childViewOptions: function () {
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
