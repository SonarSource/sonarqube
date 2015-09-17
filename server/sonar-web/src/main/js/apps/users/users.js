import Backbone from 'backbone';
import User from './user';

export default Backbone.Collection.extend({
  model: User,

  url: function () {
    return baseUrl + '/api/users/search';
  },

  parse: function (r) {
    this.total = +r.total;
    this.p = +r.p;
    this.ps = +r.ps;
    return r.users;
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
  }

});


