define [
  'backbone'
  'backbone.marionette'
  'templates/issues'
  'source-viewer/viewer'
  'issues/models/issues'
  'issues/component-viewer/issue-view'
], (
  Backbone
  Marionette
  Templates
  SourceViewer
  Issues
  IssueView
) ->

  $ = jQuery

  TOP_OFFSET = 38
  BOTTOM_OFFSET = 10


  class extends SourceViewer

    events: ->
      _.extend super,
        'click .js-close-component-viewer': 'closeComponentViewer'
        'click .sym': 'highlightUsages'
        'click .code-issue': 'selectIssue'


    initialize: (options) ->
      super
      @listenTo @, 'loaded', @onLoaded
      @listenTo options.app.state, 'change:selectedIndex', @select


    onLoaded: ->
      @bindScrollEvents()
      @bindShortcuts()
      if @baseIssue?
        @scrollToLine @baseIssue.get 'line'


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


    onClose: ->
      super
      @unbindScrollEvents()
      @unbindShortcuts()


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
      @baseIssue = issue
      componentKey = issue.get 'component'
      componentUuid = issue.get 'componentUuid'
      @open componentUuid, componentKey


    linesLimit: ->
      line = @LINES_LIMIT / 2
      if @baseIssue? && @baseIssue.has('line')
        line = Math.max line, @baseIssue.get('line')
      from: line - @LINES_LIMIT / 2 + 1
      to: line + @LINES_LIMIT / 2


    limitIssues: (issues) ->
      index = @ISSUES_LIMIT / 2
      if @baseIssue? && @baseIssue.has('index')
        index = Math.max index, @baseIssue.get('index')
      x = issues.filter (issue) =>
        Math.abs(issue.get('index') - index) <= @ISSUES_LIMIT / 2
      x


    requestIssues: ->
      console.log 'Request issues'
      if @options.app.issues.last().get('component') == @model.get('key')
        r = @options.app.controller.fetchNextPage()
      else r = $.Deferred().resolve().promise()
      r.done =>
        @issues.reset @options.app.issues.filter (issue) => issue.get('component') == @model.key()
        @issues.reset @limitIssues @issues
        @addIssuesPerLineMeta @issues
        console.log 'Issues loaded'


    renderIssue: (issue) ->
      issueView = new IssueView
        el: '#issue-' + issue.get('key')
        model: issue
        app: @options.app
      @issueViews.push issueView
      issueView.render()


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

