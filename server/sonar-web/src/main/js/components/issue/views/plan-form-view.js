define([
  'components/common/action-options-view',
  '../templates'
], function (ActionOptionsView) {

  var $ = jQuery;

  return ActionOptionsView.extend({
    template: Templates['issue-plan-form'],

    getActionPlan: function () {
      return this.model.get('actionPlan') || '';
    },

    getActionPlanName: function () {
      return this.model.get('actionPlanName');
    },

    selectInitialOption: function () {
      this.makeActive(this.getOptions().filter('[data-value="' + this.getActionPlan() + '"]'));
    },

    selectOption: function (e) {
      var actionPlan = $(e.currentTarget).data('value'),
          actionPlanName = $(e.currentTarget).data('text');
      this.submit(actionPlan, actionPlanName);
      return ActionOptionsView.prototype.selectOption.apply(this, arguments);
    },

    submit: function (actionPlan) {
      return this.model.plan(actionPlan);
    },

    getActionPlans: function () {
      return [{ key: '', name: t('issue.unplanned') }].concat(this.collection.toJSON());
    },

    serializeData: function () {
      return _.extend(ActionOptionsView.prototype.serializeData.apply(this, arguments), {
        items: this.getActionPlans()
      });
    }
  });

});
