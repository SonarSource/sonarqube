define [
  'backbone'
  'backbone.marionette'
  'templates/issues'
  'issues/models/issues'
  'issues/issue-view'
], (
  Backbone
  Marionette
  Templates
  Issues
  IssueView
) ->

  $ = jQuery

  API_SOURCES = "#{baseUrl}/api/sources/show"
  LINES_AROUND = 500
  TOP_OFFSET = 38
  BOTTOM_OFFSET = 10


  class extends Marionette.ItemView
    template: Templates['issues-component-viewer']


    ui:
      sourceBeforeSpinner: '.js-component-viewer-source-before'
      sourceAfterSpinner: '.js-component-viewer-source-after'


    events:
      'click .js-close-component-viewer': 'closeComponentViewer'


    initialize: (options) ->
      @source = new Backbone.Model
        source: []
        formattedSource: []
      @component = new Backbone.Model()
      @issues = new Issues()
      @listenTo @issues, 'add', @addIssue
      @listenTo options.app.state, 'change:selectedIndex', @select
      @bindShortcuts()
      @loadSourceBeforeThrottled = _.throttle @loadSourceBefore, 1000
      @loadSourceAfterThrottled = _.throttle @loadSourceAfter, 1000


    bindShortcuts: ->
      key 'delete,backspace,left', 'componentViewer', =>
        @options.app.controller.closeComponentViewer()
        false


    bindScrollEvents: ->
      $(window).on 'scroll.issues-component-viewer', (=> @onScroll())


    unbindScrollEvents: ->
      $(window).off 'scroll.issues-component-viewer'


    onScroll: ->
      if @model.get('hasSourceBefore') && $(window).scrollTop() <= @ui.sourceBeforeSpinner.offset().top
        @loadSourceBeforeThrottled()

      if @model.get('hasSourceAfter') && $(window).scrollTop() + $(window).height() >= @ui.sourceAfterSpinner.offset().top
        @loadSourceAfterThrottled()


    loadSourceBefore: ->
      @unbindScrollEvents()
      formattedSource = @source.get 'formattedSource'
      firstLine = _.first(formattedSource).lineNumber
      @requestSources(firstLine - LINES_AROUND, firstLine - 1).done (data) =>
        newFormattedSource = _.map data.sources, (item) => lineNumber: item[0], code: item[1]
        formattedSource = newFormattedSource.concat formattedSource
        @source.set formattedSource: formattedSource
        @model.set hasSourceBefore: newFormattedSource.length == LINES_AROUND
        @render()
        @scrollToLine firstLine
        @bindScrollEvents() if @model.get('hasSourceBefore') || @model.get('hasSourceAfter')


    loadSourceAfter: ->
      @unbindScrollEvents()
      formattedSource = @source.get 'formattedSource'
      lastLine = _.last(formattedSource).lineNumber
      @requestSources(lastLine + 1, lastLine + LINES_AROUND)
      .done (data) =>
        newFormattedSource = _.map data.sources, (item) => lineNumber: item[0], code: item[1]
        formattedSource = formattedSource.concat newFormattedSource
        @source.set formattedSource: formattedSource
        @model.set hasSourceAfter: newFormattedSource.length == LINES_AROUND
        @render()
        @bindScrollEvents() if @model.get('hasSourceBefore') || @model.get('hasSourceAfter')
      .fail =>
        @source.set formattedSource: []
        @model.set hasSourceAfter: false
        @render()


    onRender: ->
      @renderIssues()


    renderIssues: ->
      @issues.forEach (issue) =>
        @renderIssue issue


    addIssue: (issue) ->
      @renderIssue issue


    renderIssue: (issue) ->
      line = issue.get 'line' || 0
      row = @$("[data-line-number=#{line}]")
      issueView = new IssueView model: issue
      if row.length == 0
        issueView.render().$el.insertBefore @$('.issues-workspace-component-viewer-code')
      else
        row.find('.line').addClass 'issue'
        barRow = $('<tr></tr>').insertAfter row
        barCell = $('<td colspan="2"></td>').appendTo barRow
        issueView.render().$el.appendTo barCell


    select: ->
      selected = @options.app.state.get 'selectedIndex'
      selectedIssue = @options.app.issues.at selected
      if selectedIssue.get('component') == @model.get('key')
        selectedKey = selectedIssue.get 'key'
        @scrollToIssue selectedKey
        @highlightIssue selectedKey
      else
        @options.app.controller.showComponentViewer selectedIssue


    scrollToIssue: (key) ->
      el = @$("[data-issue-key='#{key}']")
      if el.length > 0
        viewTop = el.offset().top
        viewBottom = viewTop + el.outerHeight()
        windowTop = $(window).scrollTop()
        windowBottom = windowTop + $(window).height()
        if viewTop < windowTop
          $(window).scrollTop viewTop - TOP_OFFSET
        if viewBottom > windowBottom
          $(window).scrollTop $(window).scrollTop() - windowBottom + viewBottom + BOTTOM_OFFSET


    highlightIssue: (key) ->
      @$("[data-issue-key]").removeClass 'selected'
      @$("[data-issue-key='#{key}']").addClass 'selected'


    openFileByIssue: (issue) ->
      componentKey = issue.get 'component'
      @issues.reset @options.app.issues.filter (issue) => issue.get('component') == componentKey

      line = issue.get('line') || 0
      @model.set key: componentKey, issueLine: line

      @requestSources(line - LINES_AROUND, line + LINES_AROUND)
      .done (data) =>
        formattedSource = _.map data.sources, (item) => lineNumber: item[0], code: item[1]
        @source.set
          source: data.sources
          formattedSource: formattedSource
        @model.set
          hasSourceBefore: line > LINES_AROUND
          hasSourceAfter: formattedSource.length > 2 * LINES_AROUND + 1
        @render()
        @highlightIssue issue.get 'key'
        @scrollToLine issue.get 'line'
        @bindScrollEvents()
        @requestIssues()
      .fail =>
        @source.set
          source: []
          formattedSource: []
        @model.set hasSourceBefore: false, hasSourceAfter: false
        @render()
        @highlightIssue issue.get 'key'


    requestSources: (lineFrom, lineTo) ->
      lineFrom = Math.max 0, lineFrom
      $.get API_SOURCES, key: @model.get('key'), from: lineFrom, to: lineTo


    requestIssues: ->
      lastIssue = @options.app.issues.at @options.app.issues.length - 1
      lastLine = _.last(@source.get('formattedSource')).lineNumber
      needMore = !@options.app.state.get('maxResultsReached')
      needMore = needMore && (lastIssue.get('component') == @model.get 'key')
      needMore = needMore && (lastIssue.get('line') <= lastLine)
      if needMore
        @options.app.controller.fetchNextPage().done =>
          @addNextIssuesPage()
          @requestIssues()


    addNextIssuesPage: ->
      componentKey = @model.get 'key'
      @issues.add @options.app.issues.filter (issue) => issue.get('component') == componentKey


    scrollToLine: (line) ->
      row = @$("[data-line-number=#{line}]")
      goal = if row.length > 0 then row.offset().top - 40 else 0
      $(window).scrollTop goal


    closeComponentViewer: ->
      @options.app.controller.closeComponentViewer()


    serializeData: ->
      _.extend super,
        source: @source.get 'formattedSource'
