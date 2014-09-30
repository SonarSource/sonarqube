define [
  'backbone.marionette'
  'templates/dashboard'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class extends Marionette.ItemView
    template: Templates['widget']
    className: 'block'


    events:
      'click .js-edit-widget': 'editWidget'
      'click .js-cancel-edit-widget': 'cancelEditWidget'
      'submit .js-edit-widget-form': 'saveWidget'


    initialize: (options) ->
#      @listenTo options.app.state, 'change', @render
      if @model.get 'configured'
        @requestContent()


    onRender: ->
      @$el.data 'id', @model.id
      if @options.app.state.get 'configure'
        @$el.prop 'draggable', true


    requestContent: ->
      payload = id: @model.get 'key'
      if @options.app.resource
        payload.resource = @options.app.resource
      if @model.has 'componentId'
        payload.resource = @model.get 'componentId'
      _.extend payload, @getWidgetProps()
      $.get "#{baseUrl}/widget/show", payload, (html) =>
        @model.set 'html', html
        @render()


    getWidgetProps: ->
      properties = @model.get 'props'
      r = {}
      properties.forEach (prop) ->
        r[prop.key] = prop.val if prop.val?
      r


    editWidget: ->
      $.get "#{baseUrl}/api/dashboards/configure_widget", id: @model.get('key'), (data) =>
        @model.mergeProperties data.widget.properties
        @showEditForm()


    showEditForm: (render = true) ->
      @render() if render
      @$('.widget_props').removeClass 'hidden'
      @$('.configure_widget').addClass 'hidden'


    cancelEditWidget: ->
      @$('.widget_props').addClass 'hidden'
      @$('.configure_widget').removeClass 'hidden'


    saveWidget: (e) ->
      e.preventDefault()
      data = id: @model.id
      @$('.js-edit-widget-form').serializeArray().forEach (p) ->
        data[p.name] = p.value
      $.post "#{baseUrl}/api/dashboards/save_widget", data, (data) =>
        @model.set data.widget
        @requestContent()


    serializeData: ->
      _.extend super,
        baseUrl: baseUrl
        state: @options.app.state.toJSON()
