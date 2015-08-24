define([
  'backbone'
], function (Backbone) {

  return Backbone.Collection.extend({
    url: function () {
      return window.baseUrl + '/api/issues/changelog';
    },

    parse: function (r) {
      return r.changelog;
    }
  });

});
