define([
  'backbone.marionette',
  './create-view',
  './templates'
], function (Marionette, CreateView) {

  return Marionette.ItemView.extend({
    template: Templates['metrics-header'],

    events: {
      'click #metrics-create': 'onCreateClick'
    },

    onCreateClick: function (e) {
      e.preventDefault();
      this.createMetric();
    },

    createMetric: function () {
      new CreateView({
        collection: this.collection,
        domains: this.options.domains,
        types: this.options.types,
        app: this.options.app
      }).render();
    }
  });

});
