define([
  'components/issue/views/action-options-view',
  './templates'
], function (ActionOptionsView) {

  var $ = jQuery;

  return ActionOptionsView.extend({
    template: Templates['issues-issue-filter-form'],

    selectInitialOption: function () {
      return this.makeActive(this.getOptions().first());
    },

    selectOption: function (e) {
      var property = $(e.currentTarget).data('property'),
          value = $(e.currentTarget).data('value');
      this.trigger('select', property, value);
      return ActionOptionsView.prototype.selectOption.apply(this, arguments);
    },

    serializeData: function () {
      return _.extend(ActionOptionsView.prototype.serializeData.apply(this, arguments), {
        s: this.model.get('severity')
      });
    }
  });

});
