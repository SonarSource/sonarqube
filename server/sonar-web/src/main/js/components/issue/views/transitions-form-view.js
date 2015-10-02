import $ from 'jquery';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-transitions-form.hbs';

export default ActionOptionsView.extend({
  template: Template,

  selectInitialOption: function () {
    this.makeActive(this.getOptions().first());
  },

  selectOption: function (e) {
    var transition = $(e.currentTarget).data('value');
    this.submit(transition);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit: function (transition) {
    return this.model.transition(transition);
  }
});


