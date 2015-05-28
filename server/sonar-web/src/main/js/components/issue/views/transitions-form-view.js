define([
  './action-options-view',
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
      var that = this;
      return $.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/do_transition',
        data: {
          issue: this.model.get('key'),
          transition: transition
        }
      }).done(function () {
        return that.options.view.resetIssue({});
      });
    }
  });

});
