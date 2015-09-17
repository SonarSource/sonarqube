import $ from 'jquery';
import _ from 'underscore';
import ActionOptionsView from 'components/common/action-options-view';
import './templates';

export default ActionOptionsView.extend({
  template: Templates['coding-rules-rule-filter-form'],

  selectOption: function (e) {
    var property = $(e.currentTarget).data('property'),
        value = $(e.currentTarget).data('value');
    this.trigger('select', property, value);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  serializeData: function () {
    return _.extend(ActionOptionsView.prototype.serializeData.apply(this, arguments), {
      tags: _.union(this.model.get('sysTags'), this.model.get('tags'))
    });
  }

});

