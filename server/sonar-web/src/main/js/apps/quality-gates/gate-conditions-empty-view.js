define([
  './templates'
], function () {

  return Marionette.ItemView.extend({
    tagName: 'tr',
    template: Templates['quality-gate-detail-conditions-empty'],

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canEdit: this.options.canEdit
      });
    }
  });

});
