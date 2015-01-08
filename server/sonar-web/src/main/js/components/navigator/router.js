define(function () {

  return Backbone.Router.extend({
    routeSeparator: '|',

    routes: {
      '': 'index',
      ':query': 'index'
    },

    initialize: function (options) {
      this.options = options;
      this.listenTo(this.options.app.state, 'change:query', this.updateRoute);
    },

    index: function (query) {
      query = this.options.app.controller.parseQuery(query);
      this.options.app.state.setQuery(query);
    },

    updateRoute: function () {
      var route = this.options.app.controller.getRoute();
      this.navigate(route);
    }
  });

});
