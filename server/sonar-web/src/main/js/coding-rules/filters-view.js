define([
    'backbone.marionette',
    'templates/coding-rules'
], function (Marionette, Templates) {

  return Marionette.ItemView.extend({
    template: Templates['coding-rules-filters'],

    events: {
      'click .js-new-search': 'newSearch'
    },

    newSearch: function () {
      this.options.app.controller.newSearch();
    }
  });

});
