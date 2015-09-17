import _ from 'underscore';
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import ActionsView from './actions-view';
import HeaderView from './header-view';

export default Marionette.Controller.extend({
  initialize: function (options) {
    this.list = options.app.list;
    this.listenTo(this.list, 'select', this.onItemSelect);
  },

  show: function (path) {
    var that = this;
    this.fetchList().done(function () {
      if (path) {
        var item = that.list.findWhere({ path: path });
        if (item != null) {
          that.showWebService(path);
        } else {
          that.showAction(path);
        }
      }
    });
  },

  showWebService: function (path) {
    var item = this.list.findWhere({ path: path });
    if (item != null) {
      item.trigger('select', item);
    }
  },

  showAction: function (path) {
    var webService = this.list.find(function (item) {
      return path.indexOf(item.get('path')) === 0;
    });
    if (webService != null) {
      var action = path.substr(webService.get('path').length + 1);
      webService.trigger('select', webService, { trigger: false, action: action });
    }
  },

  onItemSelect: function (item, options) {
    var path = item.get('path'),
        opts = _.defaults(options || {}, { trigger: true });
    if (opts.trigger) {
      this.options.app.router.navigate(path);
    }
    this.options.app.listView.highlight(path);

    if (item.get('internal')) {
      this.options.state.set({ internal: true });
    }

    var actions = new Backbone.Collection(item.get('actions')),
        actionsView = new ActionsView({
          collection: actions,
          state: this.options.state
        });
    this.options.app.layout.detailsRegion.show(actionsView);
    this.options.app.layout.headerRegion.show(new HeaderView({ model: item }));

    if (opts.action != null) {
      actionsView.scrollToAction(opts.action);
    } else {
      actionsView.scrollToTop();
    }
  },

  fetchList: function () {
    return this.list.fetch({ data: { 'include_internals': true } });
  }
});
