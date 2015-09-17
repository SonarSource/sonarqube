import Router from 'components/navigator/router';

export default Router.extend({
  routes: {
    '': 'home',
    ':query': 'index'
  },

  initialize: function (options) {
    Router.prototype.initialize.apply(this, arguments);
    this.listenTo(options.app.state, 'change:filter', this.updateRoute);
  },

  home: function () {
    if (this.options.app.state.get('isContext')) {
      return this.navigate('resolved=false', { trigger: true, replace: true });
    } else {
      return this.options.app.controller.showHomePage();
    }
  },

  index: function (query) {
    var that = this;
    query = this.options.app.controller.parseQuery(query);
    if (query.id != null) {
      var filter = this.options.app.filters.get(query.id);
      delete query.id;
      return filter.fetch().done(function () {
        if (Object.keys(query).length > 0) {
          that.options.app.controller.applyFilter(filter, true);
          that.options.app.state.setQuery(query);
          that.options.app.state.set({ changed: true });
        } else {
          that.options.app.controller.applyFilter(filter);
        }
      });
    } else {
      return this.options.app.state.setQuery(query);
    }
  }
});


