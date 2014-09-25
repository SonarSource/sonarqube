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


    events:
      'click .js-configure-widgets': 'configureWidgets'
      'click .js-back-to-dashboard': 'stopConfigureWidgets'


    initialize: (options) ->
      @listenTo options.app.state, 'change', @render


    itemViewOptions: ->
      app: @options.app


    appendHtml: (compositeView, itemView) ->
      layout = itemView.model.get 'layout'
      column = layout.column - 1
      $container = @getItemViewContainer compositeView
      $container.eq(column).append itemView.el


    configureWidgets: ->
      @options.app.state.set configure: true


    stopConfigureWidgets: ->
      @options.app.state.set configure: false


    serializeData: ->
      _.extend super,
        dashboard: @options.dashboard.toJSON()
        manageDashboardsUrl: "#{baseUrl}/dashboards"
        state: @options.app.state.toJSON()
