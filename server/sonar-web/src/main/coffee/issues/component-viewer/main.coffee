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
  LINES_AROUND = 200
  TOP_OFFSET = 38
  BOTTOM_OFFSET = 10
  ISSUES_LIMIT = 100


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
      @listenTo options.app.state, 'change:selectedIndex', @select
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
      @hiddenIssues = false
      @issues.forEach (issue) =>
        @renderIssue issue
      if @hiddenIssues
        issues = @$('.issue').length
        warn = $('<div class="process-spinner shown">' + tp('issues.issues_limit_reached', issues) + '</div>')
        $('body').append warn
        setTimeout (-> warn.remove()), 3000


    addIssue: (issue) ->
      @renderIssue issue


    renderIssue: (issue) ->
      line = issue.get('line') || 0
      row = @$("[data-line-number=#{line}]")
      issueView = new IssueView app: @options.app, model: issue
      if line == 0
        issueView.render().$el.insertBefore @$('.issues-workspace-component-viewer-code')
      else
        showBox = Math.abs(issue.get('index') - @model.get('issueIndex')) < ISSUES_LIMIT / 2
        @hiddenIssues = true unless showBox
        if showBox && row.length > 0
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
        @unbindShortcuts()
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
      else
        @unbindShortcuts()
        selected = @options.app.state.get 'selectedIndex'
        selectedIssue = @options.app.issues.at selected
        @options.app.controller.showComponentViewer selectedIssue


    openFileByIssue: (issue) ->
      componentKey = issue.get 'component'

      line = issue.get('line') || 0
      @model.set key: componentKey, issueLine: line, issueIndex: issue.get('index')

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
        @requestIssues().done =>
          @issues.reset @options.app.issues.filter (issue) => issue.get('component') == componentKey
          @render()
          @bindScrollEvents()
          @bindShortcuts()
          @scrollToLine issue.get 'line'
      .fail =>
        @source.set
          source: []
          formattedSource: []
        @model.set hasSourceBefore: false, hasSourceAfter: false
        @issues.reset @options.app.issues.filter (issue) => issue.get('component') == componentKey
        @render()
        @bindShortcuts()
        @scrollToLine issue.get 'line'



    requestSources: (lineFrom, lineTo) ->
      lineFrom = Math.max 0, lineFrom
      $.get API_SOURCES, key: @model.get('key'), from: lineFrom, to: lineTo


    requestIssues: ->
      if @options.app.issues.last().get('component') == @model.get('key')
        @options.app.controller.fetchNextPage()
      else $.Deferred().resolve().promise()


    addNextIssuesPage: ->
      componentKey = @model.get 'key'
      @issues.add @options.app.issues.filter (issue) => issue.get('component') == componentKey


    scrollToLine: (line) ->
      row = @$("[data-line-number=#{line}]")
      goal = if row.length > 0 then row.offset().top - 200 else 0
      goal = Math.max goal, 30
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
