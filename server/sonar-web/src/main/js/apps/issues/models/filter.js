import Backbone from 'backbone';

export default Backbone.Model.extend({
  url: function () {
    return baseUrl + '/api/issue_filters/show/' + this.id;
  },

  parse: function (r) {
    if (r.filter != null) {
      return r.filter;
    } else {
      return r;
    }
  }
});


