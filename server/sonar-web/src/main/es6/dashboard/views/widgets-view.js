define([
  'backbone.marionette',
  'templates/dashboard',
  'dashboard/views/widget-view'
], function (Marionette, Templates, WidgetView) {

  class WidgetsView extends Marionette.CompositeView {

    appendHtml(compositeView, itemView) {
      var layout = itemView.model.get('layout'),
          column = layout.column - 1;
      var $container = this.getItemViewContainer(compositeView);
      $container.eq(column).append(itemView.el);
    }

    itemViewOptions() {
      return { app: this.options.app };
    }

    serializeData() {
      return _.extend(super.serializeData(), {
        dashboard: this.options.dashboard.toJSON(),
        manageDashboardsUrl: `${baseUrl}/dashboards`
      });
    }

  }

  WidgetsView.prototype.template = Templates['widgets'];
  WidgetsView.prototype.itemView = WidgetView;
  WidgetsView.prototype.itemViewContainer = '.dashboard-column';

  return WidgetsView;

});
