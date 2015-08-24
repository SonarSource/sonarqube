define([
  'backbone'
], function (Backbone) {

  return Backbone.Model.extend({
    url: function () {
      return window.baseUrl + '/api/issue_filters/show/' + this.id;
    },

    parse: function (r) {
      if (r.filter != null) {
        return r.filter;
      } else {
        return r;
      }
    }
  });

});
