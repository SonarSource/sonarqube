define [
  'backbone.marionette'
  'templates/dashboard'
  'dashboard/views/widget-view'
], (
  Marionette
  Templates
  WidgetView
) ->

  $ = jQuery


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


    onRender: ->
      @initializeDragging() if @options.app.state.get 'configure'


    initializeDragging: ->
      blocks = @$('.block')
      columnHandle = @$('.column-handle')
      draggable = null

      onDragLeave = (e) ->
        $(e.currentTarget).removeClass 'block-hover'

      onDrop = (e) =>
        e.preventDefault()
        draggable.detach().insertBefore $(e.currentTarget)
        onDragLeave e
        @options.app.saveDashboard()

      blocks.on 'selectstart', ->
        @dragDrop()
        false
      blocks.on 'dragstart', (e) ->
        e.originalEvent.dataTransfer.setData 'Text', 'drag'
        draggable = $(@)
        columnHandle.show()
      blocks.on 'dragover', (e) ->
        if draggable.data('id') != $(@).data('id')
          e.preventDefault()
          $(e.currentTarget).addClass 'block-hover'
      blocks.on 'drop', onDrop
      blocks.on 'dragleave', onDragLeave

      columnHandle.on 'dragover', (e) ->
        e.preventDefault()
        $(e.currentTarget).addClass 'block-hover'
      columnHandle.on 'drop', onDrop
      columnHandle.on 'dragleave', onDragLeave


    configureWidgets: ->
      @options.app.state.set configure: true


    stopConfigureWidgets: ->
      @options.app.state.set configure: false


    getLayout: ->
      layout = $('.dashboard-column').map( ->
          blocks = $(@).find '.block'
          blocks.map( -> $(@).data('id')).get().join(',')
      ).get().join(';')


    serializeData: ->
      _.extend super,
        dashboard: @options.dashboard.toJSON()
        manageDashboardsUrl: "#{baseUrl}/dashboards"
        state: @options.app.state.toJSON()
