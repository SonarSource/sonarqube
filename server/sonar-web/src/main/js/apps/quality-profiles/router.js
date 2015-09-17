import Backbone from 'backbone';

export default Backbone.Router.extend({
  routes: {
    '': 'index',
    'index': 'index',
    'show?key=:key': 'show',
    'changelog*': 'changelog',
    'compare*': 'compare'
  },

  initialize: function (options) {
    this.app = options.app;
  },

  index: function () {
    this.app.controller.index();
  },

  show: function (key) {
    this.app.controller.show(key);
  },

  changelog: function () {
    var params = window.getQueryParams();
    this.app.controller.changelog(params.key, params.since, params.to);
  },

  compare: function () {
    var params = window.getQueryParams();
    if (params.key && params.withKey) {
      this.app.controller.compare(params.key, params.withKey);
    }
  }
});


