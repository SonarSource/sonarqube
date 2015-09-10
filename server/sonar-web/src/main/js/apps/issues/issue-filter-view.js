define([
  'components/common/action-options-view',
  './templates'
], function (ActionOptionsView) {

  var $ = jQuery;

  return ActionOptionsView.extend({
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

});
