import $ from 'jquery';
import _ from 'underscore';
import Item from './models/item';
import Items from './models/items';
import ItemsView from './views/items-view';
import ViewerView from './views/viewer-view';
import RuleView from './views/rule-view';


var instance = null,

    Workspace = function () {
      if (instance != null) {
        throw new Error('Cannot instantiate more than one Workspace, use Workspace.getInstance()');
      }
      this.initialize();
    };

Workspace.prototype = {
  initialize: function () {
    var that = this;

    this.items = new Items();
    this.items.load();
    this.items.on('change', function () {
      that.save();
    });

    this.itemsView = new ItemsView({ collection: this.items });
    this.itemsView.render().$el.appendTo(document.body);
    this.itemsView.on('click', function (model) {
      that.open(model);
    });
  },

  save: function () {
    this.items.save();
  },

  addComponent: function (model) {
    var m = this.items.add2(model);
    this.save();
    return m;
  },

  open: function (options) {
    var model = typeof options.toJSON === 'function' ? options : new Item(options);
    if (!model.isValid()) {
      throw new Error(model.validationError);
    }
    var m = this.addComponent(model);
    if (m.isComponent()) {
      this.showComponentViewer(m);
    }
    if (m.isRule()) {
      this.showRule(m);
    }
  },

  openComponent: function (options) {
    return this.open(_.extend(options, { type: 'component' }));
  },

  openRule: function (options) {
    return this.open(_.extend(options, { type: 'rule' }));
  },

  showViewer: function (Viewer, model) {
    var that = this;
    if (this.viewerView != null) {
      this.viewerView.model.trigger('hideViewer');
      this.viewerView.destroy();
    }
    $('html').addClass('with-workspace');
    model.trigger('showViewer');
    this.viewerView = new Viewer({ model: model });
    this.viewerView
        .on('viewerMinimize', function () {
          model.trigger('hideViewer');
          that.closeComponentViewer();
        })
        .on('viewerClose', function (m) {
          that.closeComponentViewer();
          m.destroy();
        });
    this.viewerView.render().$el.appendTo(document.body);
  },

  showComponentViewer: function (model) {
    this.showViewer(ViewerView, model);
  },

  closeComponentViewer: function () {
    if (this.viewerView != null) {
      this.viewerView.destroy();
      $('.with-workspace').removeClass('with-workspace');
    }
  },

  showRule: function (model) {
    var that = this;
    this.fetchRule(model)
        .done(function () {
          model.set({ exist: true });
          that.showViewer(RuleView, model);
        }).fail(function () {
          model.set({ exist: false });
          that.showViewer(RuleView, model);
        });
  },

  fetchRule: function (model) {
    var url = baseUrl + '/api/rules/show',
        options = { key: model.get('key') };
    return $.get(url, options).done(function (r) {
      model.set(r.rule);
    });
  }
};

Workspace.getInstance = function () {
  if (instance == null) {
    instance = new Workspace();
  }
  return instance;
};

export default Workspace.getInstance();


