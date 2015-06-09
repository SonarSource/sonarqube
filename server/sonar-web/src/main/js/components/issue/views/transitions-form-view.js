define([
  'components/common/action-options-view',
  '../templates'
], function (ActionOptionsView) {

  var $ = jQuery;

  return ActionOptionsView.extend({
    template: Templates['issue-transitions-form'],

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

});
