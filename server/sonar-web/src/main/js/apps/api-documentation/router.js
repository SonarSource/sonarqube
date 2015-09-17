import Backbone from 'backbone';

export default Backbone.Router.extend({
  routes: {
    '*path': 'show'
  },

  initialize: function (options) {
    this.app = options.app;
  },

  show: function (path) {
    this.app.controller.show(path);
  }
});


