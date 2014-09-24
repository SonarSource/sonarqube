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

    initialize: ->
        @requestContent()


    requestContent: ->
      payload = id: @model.id
      if @options.app.resource
        payload.resource = @options.app.resource
      _.extend payload, @getWidgetProps()
      $.get "#{baseUrl}/widget/show", payload, (html) =>
        @model.set 'html', html
        @render()


    getWidgetProps: ->
      props = @model.get 'props'
      r = {}
      props.forEach (prop) ->
        r[prop.key] = prop.value
      r
