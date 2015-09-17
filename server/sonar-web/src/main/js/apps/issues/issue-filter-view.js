import $ from 'jquery';
import _ from 'underscore';
import ActionOptionsView from 'components/common/action-options-view';
import './templates';

export default ActionOptionsView.extend({
  template: Templates['issues-issue-filter-form'],

  selectOption: function (e) {
    var property = $(e.currentTarget).data('property'),
        value = $(e.currentTarget).data('value');
    this.trigger('select', property, value);
    this._super(e);
  },

  serializeData: function () {
    return _.extend(this._super(), {
      s: this.model.get('severity')
    });
  }
});


