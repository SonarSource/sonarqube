define([], function () {

  return Backbone.Collection.extend({
    url: function () {
      return baseUrl + '/api/issues/changelog';
    },

    parse: function (r) {
      return r.changelog;
    }
  });

});
