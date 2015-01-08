define ->

  class AppRouter extends Backbone.Router

    routes:
      '': 'showCurrent'
      'current': 'showCurrent'
      'past': 'showPast'


    initialize: (options) ->
      @options = options


    showCurrent: ->
      @navigate 'current'
      @options.app.state.set active: true


    showPast: ->
      @navigate 'past'
      @options.app.state.set active: false
