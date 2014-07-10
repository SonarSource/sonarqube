define [
  'backbone.marionette'
], (
  Marionette
) ->

  class extends Marionette.ItemView

    initialize: (options) ->
      super
      @main = options.main
      @state = options.state
      @component = options.component
      @settings = options.settings
      @source = options.source
      @header = options.header


    serializeData: ->
      _.extend super,
        state: @state.toJSON()
        component: @component.toJSON()
        settings: @settings.toJSON()
        periods: @main.periods.toJSON()