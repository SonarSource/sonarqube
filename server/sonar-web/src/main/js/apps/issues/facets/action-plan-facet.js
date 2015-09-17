import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['issues-action-plan-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').planned;
    if ((value != null) && (!value || value === 'false')) {
      return this.$('.js-facet').filter('[data-unplanned]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var unplanned = $(e.currentTarget).is('[data-unplanned]');
    $(e.currentTarget).toggleClass('active');
    if (unplanned) {
      var checked = $(e.currentTarget).is('.active'),
          value = checked ? 'false' : null;
      return this.options.app.state.updateFilter({
        planned: value,
        actionPlans: null
      });
    } else {
      return this.options.app.state.updateFilter({
        planned: null,
        actionPlans: this.getValue()
      });
    }
  },

  getValuesWithLabels: function () {
    var values = this.model.getValues(),
        actionPlans = this.options.app.facets.actionPlans;
    values.forEach(function (v) {
      var key = v.val,
          label = null;
      if (key) {
        var actionPlan = _.findWhere(actionPlans, { key: key });
        if (actionPlan != null) {
          label = actionPlan.name;
        }
      }
      v.label = label;
    });
    return values;
  },

  disable: function () {
    return this.options.app.state.updateFilter({
      planned: null,
      actionPlans: null
    });
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValuesWithLabels()
    });
  }
});


