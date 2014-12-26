define([
  'backbone.marionette',
  'templates/coding-rules',
  'coding-rules/rule/custom-rule-view'
], function (Marionette, Templates, CustomRuleView) {

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

    onRender: function () {
      this.$el.toggleClass('hidden', !this.model.get('isTemplate'));
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
