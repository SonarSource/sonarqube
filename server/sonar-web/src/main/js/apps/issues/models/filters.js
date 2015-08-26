define([
  'backbone',
  './filter'
], function (Backbone, Filter) {

  return Backbone.Collection.extend({
    model: Filter,

    url: function () {
      return window.baseUrl + '/api/issue_filters/search';
    },

    parse: function (r) {
      return r.issueFilters;
    }
  });

});
