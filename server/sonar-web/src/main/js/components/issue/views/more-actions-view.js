import $ from 'jquery';
import ActionOptionsView from 'components/common/action-options-view';
import '../templates';

export default ActionOptionsView.extend({
  template: Templates['issue-more-actions'],

  selectOption: function (e) {
    var action = $(e.currentTarget).data('action');
    this.submit(action);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit: function (actionKey) {
    return this.options.detailView.action(actionKey);
  }
});


