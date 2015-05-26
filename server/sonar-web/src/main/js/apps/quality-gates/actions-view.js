define([
  './create-view',
  './templates'
], function (CreateView) {

  return Marionette.ItemView.extend({
    template: Templates['quality-gate-actions'],

    events: {
      'click #quality-gate-add': 'add'
    },

    add: function (e) {
      e.preventDefault();
      new CreateView({
        collection: this.collection
      }).render();
    },

    serializeData: function () {
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        canEdit: this.options.canEdit
      });
    }
  });

});
