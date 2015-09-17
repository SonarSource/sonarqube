import _ from 'underscore';
import Backbone from 'backbone';
import Project from './project';

export default Backbone.Collection.extend({
  model: Project,

  url: function () {
    return baseUrl + '/api/projects/provisioned';
  },

  parse: function (r) {
    this.total = r.total;
    this.p = r.p;
    this.ps = r.ps;
    return r.projects;
  },

  fetch: function (options) {
    var d = (options && options.data) || {};
    this.q = d.q;
    return this._super(options);
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
  },

  bulkDelete: function (ids, options) {
    var opts = _.extend({}, options, {
      type: 'POST',
      url: baseUrl + '/api/projects/bulk_delete',
      data: { ids: ids.join() }
    });
    return Backbone.ajax(opts);
  }

});


