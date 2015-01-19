define([
    'templates/coding-rules'
], function () {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-filters'],

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canWrite: this.options.app.canWrite
      });
    }
  });

});
