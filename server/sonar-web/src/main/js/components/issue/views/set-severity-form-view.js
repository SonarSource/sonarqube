define([
  './action-options-view',
  '../templates'
], function (ActionOptionsView) {

  var $ = jQuery;

  return ActionOptionsView.extend({
    template: Templates['issue-set-severity-form'],

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
      var that = this;
      var _severity = this.getTransition();
      if (severity === _severity) {
        return;
      }
      this.model.set({ severity: severity });
      return $.ajax({
        type: 'POST',
        url: baseUrl + '/api/issues/set_severity',
        data: {
          issue: this.model.id,
          severity: severity
        }
      }).fail(function () {
        that.model.set({ severity: _severity });
      });
    },

    serializeData: function () {
      return _.extend(ActionOptionsView.prototype.serializeData.apply(this, arguments), {
        items: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
      });
    }
  });

});
