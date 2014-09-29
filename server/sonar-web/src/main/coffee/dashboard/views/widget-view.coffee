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
      @requestContent()


    onRender: ->
      @$el.data 'id', @model.id
      if @options.app.state.get 'configure'
        @$el.prop 'draggable', true


    requestContent: ->
      payload = id: @model.get 'key'
      if @options.app.resource
        payload.resource = @options.app.resource
      _.extend payload, @getWidgetProps()
      $.get "#{baseUrl}/widget/show", payload, (html) =>
        @model.set 'html', html
        @render()


    getWidgetProps: ->
      properties = @model.get 'properties'
      r = {}
      properties.forEach (prop) ->
        r[prop.key] = prop.value if prop.value?
      r


    editWidget: ->
      $.get "#{baseUrl}/api/dashboards/configure_widget", id: @model.get('key'), (data) =>
        @model.mergeProperties data.widget.properties
        @showEditForm()


    showEditForm: ->
      @render()
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
