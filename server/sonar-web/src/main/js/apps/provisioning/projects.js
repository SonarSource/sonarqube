define([
  './project'
], function (Project) {

  return Backbone.Collection.extend({
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
      return Backbone.Collection.prototype.fetch.apply(this, arguments);
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
