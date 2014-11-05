define [
  'backbone.marionette'
  'templates/issues'
  'issues/workspace-list-item-view'
], (
  Marionette
  Templates
  IssueView
) ->

  $ = jQuery

  TOP_OFFSET = 38
  BOTTOM_OFFSET = 10


  class extends Marionette.CompositeView
    template: Templates['issues-workspace-list']
    itemView: IssueView
    itemViewContainer: 'ul'


    ui:
      loadMore: '.js-issues-more'


    itemViewOptions: (_, index) ->
      app: @options.app
      index: index


    collectionEvents:
      'reset': 'scrollToTop'


    initialize: ->
      @loadMoreThrottled = _.throttle @loadMore, 1000
      @listenTo @options.app.state, 'change:maxResultsReached', @toggleLoadMore
      @listenTo @options.app.state, 'change:selectedIndex', @scrollToIssue
      @bindShortcuts()


    onClose: ->
      @unbindScrollEvents()


    toggleLoadMore: ->
      @ui.loadMore.toggle !@options.app.state.get 'maxResultsReached'


    bindScrollEvents: ->
      $(window).on 'scroll.issues-workspace-list', (=> @onScroll())


    unbindScrollEvents: ->
      $(window).off 'scroll.issues-workspace-list'


    bindShortcuts: ->
      doTransition = (transition) =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        return unless selectedIssue?
        selectedIssueView = @children.findByModel selectedIssue
        selectedIssueView.$("[data-transition=#{transition}]").click()

      doAction = (action) =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        return unless selectedIssue?
        selectedIssueView = @children.findByModel selectedIssue
        selectedIssueView.$("#issue-#{action}").click()

      key 'right', 'list', =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        @options.app.controller.showComponentViewer selectedIssue
        return false

      key 'c', -> doTransition 'confirm'
      key 'u', -> doTransition 'unconfirm'
      key 'r', -> doTransition 'resolve'
      key 'r', -> doTransition 'reopen'
      key 'f', -> doTransition 'falsepositive'

      key 'a', -> doAction 'assign'
      key 'm', -> doAction 'assign-to-me'
      key 'p', -> doAction 'plan'
      key 'i', -> doAction 'set-severity'


    loadMore: ->
      unless @options.app.state.get 'maxResultsReached'
        @unbindScrollEvents()
        @options.app.controller.fetchNextPage().done => @bindScrollEvents()


    onScroll: ->
      if $(window).scrollTop() + $(window).height() >= @ui.loadMore.offset().top
        @loadMoreThrottled()


    scrollToTop: ->
      @$el.scrollParent().scrollTop 0


    scrollToIssue: ->
      selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
      return unless selectedIssue?
      selectedIssueView = @children.findByModel selectedIssue
      viewTop = selectedIssueView.$el.offset().top
      viewBottom = viewTop + selectedIssueView.$el.outerHeight()
      windowTop = $(window).scrollTop()
      windowBottom = windowTop + $(window).height()
      if viewTop < windowTop
        $(window).scrollTop viewTop - TOP_OFFSET
      if viewBottom > windowBottom
        $(window).scrollTop $(window).scrollTop() - windowBottom + viewBottom + BOTTOM_OFFSET


