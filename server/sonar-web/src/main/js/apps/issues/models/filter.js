define(function () {

  return Backbone.Model.extend({
    url: function () {
      return '/api/issue_filters/show/' + this.id;
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
