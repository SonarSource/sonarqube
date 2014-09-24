define [
  'backbone.marionette'
  'templates/dashboard'
  'dashboard/views/widget-view'
], (
  Marionette
  Templates
  WidgetView
) ->

  class extends Marionette.CompositeView
    template: Templates['widgets']
    itemView: WidgetView
    itemViewContainer: '.dashboard-column'


    itemViewOptions: ->
      { app: @options.app }


    appendHtml: (compositeView, itemView) ->
      layout = itemView.model.get 'layout'
      column = layout.column - 1
      $container = @getItemViewContainer compositeView
      $container.eq(column).append itemView.el


    serializeData: ->
      _.extend super,
        dashboard: @options.dashboard.toJSON()
        manageDashboardsUrl: "#{baseUrl}/dashboards"
