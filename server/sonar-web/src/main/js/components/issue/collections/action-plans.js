define([
  'backbone'
], function (Backbone) {

  return Backbone.Collection.extend({
    url: function () {
      return window.baseUrl + '/api/action_plans/search';
    },

    parse: function (r) {
      return r.actionPlans;
    }
  });

});
