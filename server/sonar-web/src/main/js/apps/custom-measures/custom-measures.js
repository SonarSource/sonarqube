define([
  './custom-measure'
], function (CustomMeasure) {

  return Backbone.Collection.extend({
    model: CustomMeasure,

    initialize: function (options) {
      this.projectId = options.projectId;
    },

    url: function () {
      return baseUrl + '/api/custom_measures/search';
    },

    parse: function (r) {
      this.total = r.total;
      this.p = r.p;
      this.ps = r.ps;
      return r.customMeasures;
    },

    fetch: function (options) {
      var opts = _.defaults(options || {}, { data: {} });
      this.q = opts.data.q;
      opts.data.projectId = this.projectId;
      return this._super(opts);
    },

    fetchMore: function () {
      var p = this.p + 1;
      return this.fetch({ add: true, remove: false, data: { p: p, ps: this.ps, q: this.q } });
    },

    refresh: function () {
      return this.fetch({ reset: true, data: { q: this.q } });
    },

    hasMore: function () {
      return this.total > this.p * this.ps;
    }

  });

});
