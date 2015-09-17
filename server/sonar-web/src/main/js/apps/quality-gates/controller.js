import _ from 'underscore';
import Marionette from 'backbone.marionette';
import DetailsView from './details-view';
import HeaderView from './header-view';

export default Marionette.Controller.extend({

  initialize: function (options) {
    this.app = options.app;
    this.canEdit = this.app.canEdit;
    this.listenTo(this.app.gates, 'select', this.onSelect);
    this.listenTo(this.app.gates, 'destroy', this.onDestroy);
  },

  index: function () {
    this.app.gates.fetch();
  },

  show: function (id) {
    var that = this;
    this.app.gates.fetch().done(function () {
      var gate = that.app.gates.get(id);
      if (gate != null) {
        gate.trigger('select', gate, { trigger: false });
      }
    });
  },

  onSelect: function (gate, options) {
    var that = this,
        route = 'show/' + gate.id,
        opts = _.defaults(options || {}, { trigger: true });
    if (opts.trigger) {
      this.app.router.navigate(route);
    }
    this.app.gatesView.highlight(gate.id);
    gate.fetch().done(function () {
      var headerView = new HeaderView({
        model: gate,
        canEdit: that.canEdit
      });
      that.app.layout.headerRegion.show(headerView);

      var detailsView = new DetailsView({
        model: gate,
        canEdit: that.canEdit,
        metrics: that.app.metrics,
        periods: that.app.periods
      });
      that.app.layout.detailsRegion.show(detailsView);
    });
  },

  onDestroy: function () {
    this.app.router.navigate('');
    this.app.layout.headerRegion.reset();
    this.app.layout.detailsRegion.reset();
    this.app.layout.renderIntro();
    this.app.gatesView.highlight(null);
  }

});


