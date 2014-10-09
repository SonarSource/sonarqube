define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    template: Templates['analysis-reports-actions']


    events:
      'click .js-show-past-reports': 'showPastReports'
      'click .js-show-current-activity': 'showCurrentActivity'


    initialize: (options) ->
      @listenTo options.collection, 'all', @render
      @listenTo options.app.state, 'change', @render


    showPastReports: ->
      @options.app.state.set active: false


    showCurrentActivity: ->
      @options.app.state.set active: true


    serializeData: ->
      _.extend super,
        state: @options.app.state.toJSON()
        total: if @collection.paging then @collection.paging.total else @collection.length
