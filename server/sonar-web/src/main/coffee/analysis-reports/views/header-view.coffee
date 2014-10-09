define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    template: Templates['analysis-reports-header']
    

    events:
      'click .js-analysis-reports-past': 'showPastReports'
      'click .js-analysis-reports-current': 'showCurrentActivity'


    initialize: (options) ->
      @listenTo options.app.state, 'change', @render


    showPastReports: ->
      @options.app.state.set active: false


    showCurrentActivity: ->
      @options.app.state.set active: true


    serializeData: ->
      _.extend super,
        state: @options.app.state.toJSON()
