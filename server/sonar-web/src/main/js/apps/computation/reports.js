import _ from 'underscore';
import Backbone from 'backbone';
import Report from './report';

export default Backbone.Collection.extend({
  model: Report,
  url: '',

  parse: function (r) {
    this.total = (r.paging && r.paging.total) || r.tasks.length;
    this.p = (r.paging && r.paging.pageIndex) || 1;
    this.ps = r.paging && r.paging.pageSize;
    return r.tasks;
  },

  fetch: function (options) {
    var opts = _.defaults(options || {}, { q: this.q }, { q: 'activity', data: { ps: 250 } });
    opts.url = baseUrl + '/api/ce/' + opts.q;
    this.q = opts.q;
    return Backbone.Collection.prototype.fetch.call(this, opts);
  },

  fetchMore: function () {
    var p = this.p + 1;
    return this.fetch({ add: true, remove: false, data: { p: p, ps: this.ps } });
  },

  hasMore: function () {
    return this.total > this.p * this.ps;
  }

});

