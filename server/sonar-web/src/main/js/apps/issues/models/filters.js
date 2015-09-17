import Backbone from 'backbone';
import Filter from './filter';

export default Backbone.Collection.extend({
  model: Filter,

  url: function () {
    return window.baseUrl + '/api/issue_filters/search';
  },

  parse: function (r) {
    return r.issueFilters;
  }
});


