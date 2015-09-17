import _ from 'underscore';
import Backbone from 'backbone';
import Metric from './metric';

export default Backbone.Collection.extend({
  model: Metric,

  url: function () {
    return baseUrl + '/api/metrics/search';
  },

  parse: function (r) {
    this.total = r.total;
    this.p = r.p;
    this.ps = r.ps;
    return r.metrics;
  },

  fetch: function (options) {
    var opts = _.defaults(options || {}, { data: {} });
    this.q = opts.data.q;
    opts.data.isCustom = true;
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


