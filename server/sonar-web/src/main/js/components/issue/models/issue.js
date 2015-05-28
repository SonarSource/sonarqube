define([], function () {

  return Backbone.Model.extend({
    idAttribute: 'key',

    url: function () {
      return baseUrl + '/api/issues/show?key=' + this.get('key');
    },

    parse: function (r) {
      return r.issue ? r.issue : r;
    }
  });

});
