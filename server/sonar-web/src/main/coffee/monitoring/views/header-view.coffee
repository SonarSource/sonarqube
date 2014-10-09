define [
  'backbone.marionette'
  'templates/monitoring'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    template: Templates['monitoring-header']
    

    events:
      'click .js-monitoring-past': 'showPastReports'
      'click .js-monitoring-current': 'showCurrentActivity'


    initialize: (options) ->
      @listenTo options.app.state, 'change', @render


    showPastReports: ->
      @options.app.state.set active: false


    showCurrentActivity: ->
      @options.app.state.set active: true


    serializeData: ->
      _.extend super,
        state: @options.app.state.toJSON()
