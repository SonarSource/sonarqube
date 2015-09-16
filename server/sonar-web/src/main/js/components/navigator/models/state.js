define(function () {

  return Backbone.Model.extend({
    defaults: function () {
      return {
        page: 1,
        maxResultsReached: false,
        query: {},
        facets: []
      };
    },

    nextPage: function () {
      var page = this.get('page');
      this.set({ page: page + 1 });
    },

    clearQuery: function (query) {
      var q = {};
      Object.keys(query).forEach(function (key) {
        if (query[key]) {
          q[key] = query[key];
        }
      });
      return q;
    },

    _areQueriesEqual: function (a, b) {
      var equal = Object.keys(a).length === Object.keys(b).length;
      Object.keys(a).forEach(function (key) {
        equal = equal && a[key] === b[key];
      });
      return equal;
    },

    updateFilter: function (obj, options) {
      var oldQuery = this.get('query'),
          query = _.extend({}, oldQuery, obj),
          opts = _.defaults(options || {}, { force: false });
      query = this.clearQuery(query);
      if (opts.force || !this._areQueriesEqual(oldQuery, query)) {
        this.setQuery(query);
      }
    },

    setQuery: function (query) {
      this.set({ query: query }, { silent: true });
      this.set({ changed: true });
      this.trigger('change:query');
    }
  });

});
