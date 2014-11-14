define [
  'backbone'
  'backbone.marionette'
  'templates/issues'
  'issues/models/issues'
  'issues/component-viewer/issue-view'
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
      'click .sym': 'highlightUsages'
      'click .code-issue': 'selectIssue'


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
      @scrollTimer = null


    bindShortcuts: ->
      doTransition = (transition) =>
        selectedIssueView = @getSelectedIssueEl()
        return unless selectedIssueView
        selectedIssueView.find("[data-transition=#{transition}]").click()

      doAction = (action) =>
        selectedIssueView = @getSelectedIssueEl()
        return unless selectedIssueView
        selectedIssueView.find(".js-issue-#{action}").click()

      key 'up', 'componentViewer', =>
        @options.app.controller.selectPreviousIssue()
        false

      key 'down', 'componentViewer', =>
        @options.app.controller.selectNextIssue()
        false

      key 'left', 'componentViewer', =>
        @options.app.controller.closeComponentViewer()
        false

      key 'c', 'componentViewer', -> doTransition 'confirm'
      key 'u', 'componentViewer', -> doTransition 'unconfirm'
      key 'r', 'componentViewer', -> doTransition 'resolve'
      key 'r', 'componentViewer', -> doTransition 'reopen'
      key 'f', 'componentViewer', -> doTransition 'falsepositive'
      key 'a', 'componentViewer', -> doAction 'assign'
      key 'm', 'componentViewer', -> doAction 'assign-to-me'
      key 'p', 'componentViewer', -> doAction 'plan'
      key 'i', 'componentViewer', -> doAction 'set-severity'
      key 'o', 'componentViewer', -> doAction 'comment'


    unbindShortcuts: ->
      key.deleteScope 'componentViewer'


    bindScrollEvents: ->
      $(window).on 'scroll.issues-component-viewer', (=> @onScroll())


    unbindScrollEvents: ->
      $(window).off 'scroll.issues-component-viewer'


    disablePointerEvents: ->
      clearTimeout @scrollTimer
      $('body').addClass 'disabled-pointer-events'
      @scrollTimer = setTimeout (-> $('body').removeClass 'disabled-pointer-events'), 250


    onScroll: ->
      @disablePointerEvents()

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


    onClose: ->
      @unbindScrollEvents()
      @unbindShortcuts()


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
      issueView = new IssueView app: @options.app, model: issue
      if row.length == 0
        issueView.render().$el.insertBefore @$('.issues-workspace-component-viewer-code')
      else
        line = row.find '.line'
        line.addClass 'has-issues'
        issues = line.find '.issue-list'
        if issues.length == 0
          issues = $('<div class="issue-list"></div>').appendTo line
        issueView.render().$el.appendTo issues


    select: ->
      selected = @options.app.state.get 'selectedIndex'
      selectedIssue = @options.app.issues.at selected
      if selectedIssue.get('component') == @model.get('key')
        selectedKey = selectedIssue.get 'key'
        @scrollToIssue selectedKey
      else
        @options.app.controller.showComponentViewer selectedIssue


    getSelectedIssueEl: ->
      selected = @options.app.state.get 'selectedIndex'
      return null unless selected?
      selectedIssue = @options.app.issues.at selected
      return null unless selectedIssue?
      selectedIssueView = @$("#issue-#{selectedIssue.get('key')}")
      if selectedIssueView.length > 0 then selectedIssueView else null


    selectIssue: (e) ->
      key = $(e.currentTarget).data 'issue-key'
      issue = @issues.find (issue) -> issue.get('key') == key
      index = @options.app.issues.indexOf issue
      @options.app.state.set selectedIndex: index


    scrollToIssue: (key) ->
      el = @$("#issue-#{key}")
      if el.length > 0
        viewTop = el.offset().top
        viewBottom = viewTop + el.outerHeight()
        windowTop = $(window).scrollTop()
        windowBottom = windowTop + $(window).height()
        if viewTop < windowTop
          $(window).scrollTop viewTop - TOP_OFFSET
        if viewBottom > windowBottom
          $(window).scrollTop $(window).scrollTop() - windowBottom + viewBottom + BOTTOM_OFFSET


    openFileByIssue: (issue) ->
      componentKey = issue.get 'component'
      @issues.reset @options.app.issues.filter (issue) => issue.get('component') == componentKey

      line = issue.get('line') || 0
      @model.set key: componentKey, issueLine: line

      # disable clipping effect of the black header
      @$el.addClass 'full-height'

      @requestSources(line - LINES_AROUND, line + LINES_AROUND)
      .done (data) =>
        formattedSource = _.map data.sources, (item) => lineNumber: item[0], code: item[1]
        @source.set
          source: data.sources
          formattedSource: formattedSource
        firstLine = _.first(formattedSource).lineNumber
        lastLine = _.last(formattedSource).lineNumber
        @model.set
          hasSourceBefore: firstLine > 1
          hasSourceAfter: lastLine == line + LINES_AROUND
        @render()
        @bindScrollEvents()
        @requestIssues()
      .fail =>
        @source.set
          source: []
          formattedSource: []
        @model.set hasSourceBefore: false, hasSourceAfter: false
        @render()
      .always =>
        @$el.removeClass 'full-height'
        @scrollToLine issue.get 'line'


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


    highlightUsages: (e) ->
      highlighted = $(e.currentTarget).is '.highlighted'
      key = e.currentTarget.className.split(/\s+/)[0]
      @$('.sym.highlighted').removeClass 'highlighted'
      @$(".sym.#{key}").addClass 'highlighted' unless highlighted


    serializeData: ->
      _.extend super,
        source: @source.get 'formattedSource'
