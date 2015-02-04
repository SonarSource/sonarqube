define [
  'issue/issue-view'
], (
  IssueView
) ->

  class extends IssueView

    events: ->
      _.extend super,
        'click': 'selectCurrent'
        'dblclick': 'openComponentViewer'
        'click .js-issue-navigate': 'openComponentViewer'


    initialize: (options) ->
      super
      @listenTo options.app.state, 'change:selectedIndex', @select


    onRender: ->
      super
      @select()
      @$el.addClass 'issue-navigate-right'


    select: ->
      selected = @model.get('index') == @options.app.state.get 'selectedIndex'
      @$el.toggleClass 'selected', selected


    selectCurrent: ->
      @options.app.state.set selectedIndex: @model.get('index')


    resetIssue: (options) ->
      key = @model.get 'key'
      componentUuid = @model.get 'componentUuid'
      index = @model.get 'index'
      @model.clear silent: true
      @model.set { key: key, componentUuid: componentUuid, index: index }, { silent: true }
      @model.fetch(options)
      .done =>
        @trigger 'reset'


    openComponentViewer: ->
      @options.app.state.set selectedIndex: @model.get('index')
      if @options.app.state.has 'component'
        @options.app.controller.closeComponentViewer()
      else
        @options.app.controller.showComponentViewer @model


    serializeData: ->
      _.extend super,
        showComponent: true
