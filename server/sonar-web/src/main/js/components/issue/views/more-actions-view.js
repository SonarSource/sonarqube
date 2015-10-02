import $ from 'jquery';
import ActionOptionsView from '../../../components/common/action-options-view';
import Template from '../templates/issue-more-actions.hbs';

export default ActionOptionsView.extend({
  template: Template,

  selectOption: function (e) {
    var action = $(e.currentTarget).data('action');
    this.submit(action);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit: function (actionKey) {
    return this.options.detailView.action(actionKey);
  }
});


