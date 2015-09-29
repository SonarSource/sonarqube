import $ from 'jquery';
import _ from 'underscore';
import ActionOptionsView from 'components/common/action-options-view';
import '../templates';

export default ActionOptionsView.extend({
  template: Templates['issue-plan-form'],

  getActionPlan: function () {
    return this.model.get('actionPlan') || '';
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


