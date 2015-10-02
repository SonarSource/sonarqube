import $ from 'jquery';
import _ from 'underscore';
import ActionOptionsView from '../../common/action-options-view';
import Template from '../templates/issue-set-severity-form.hbs';

export default ActionOptionsView.extend({
  template: Template,

  getTransition: function () {
    return this.model.get('severity');
  },

  selectInitialOption: function () {
    return this.makeActive(this.getOptions().filter('[data-value="' + this.getTransition() + '"]'));
  },

  selectOption: function (e) {
    var severity = $(e.currentTarget).data('value');
    this.submit(severity);
    return ActionOptionsView.prototype.selectOption.apply(this, arguments);
  },

  submit: function (severity) {
    return this.model.setSeverity(severity);
  },

  serializeData: function () {
    return _.extend(ActionOptionsView.prototype.serializeData.apply(this, arguments), {
      items: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
    });
  }
});


