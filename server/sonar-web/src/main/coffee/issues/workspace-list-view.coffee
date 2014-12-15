define [
  'backbone.marionette'
  'templates/issues'
  'issues/workspace-list-item-view'
  'issues/workspace-list-empty-view'
], (
  Marionette
  Templates
  IssueView
  EmptyView
) ->

  $ = jQuery

  TOP_OFFSET = 43
  COMPONENT_HEIGHT = 29
  BOTTOM_OFFSET = 10


  class extends Marionette.CompositeView
    template: Templates['issues-workspace-list']
    componentTemplate: Templates['issues-workspace-list-component']
    itemView: IssueView
    itemViewContainer: '.js-issues-list'
    emptyView: EmptyView


    ui:
      loadMore: '.js-issues-more'


    itemViewOptions: ->
      app: @options.app


    collectionEvents:
      'reset': 'scrollToTop'


    initialize: ->
      @loadMoreThrottled = _.throttle @loadMore, 1000, { trailing: false }
      @listenTo @options.app.state, 'change:maxResultsReached', @toggleLoadMore
      @listenTo @options.app.state, 'change:selectedIndex', @scrollToIssue
      @bindShortcuts()


    onClose: ->
      @unbindScrollEvents()
      @unbindShortcuts()


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
        selectedIssueView.$(".js-issue-#{action}").click()

      key 'up', 'list', =>
        @options.app.controller.selectPreviousIssue()
        false

      key 'down', 'list', =>
        @options.app.controller.selectNextIssue()
        false

      key 'right', 'list', =>
        selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
        @options.app.controller.showComponentViewer selectedIssue
        return false

      key 'c', 'list', -> doTransition 'confirm'
      key 'c', 'list', -> doTransition 'unconfirm'
      key 'u', 'list', -> doTransition 'mute'
      key 'r', 'list', -> doTransition 'resolve'
      key 'r', 'list', -> doTransition 'reopen'
      key 'f', 'list', -> doTransition 'falsepositive'
      key 'a', 'list', -> doAction 'assign'
      key 'm', 'list', -> doAction 'assign-to-me'
      key 'p', 'list', -> doAction 'plan'
      key 'i', 'list', -> doAction 'set-severity'
      key 'o', 'list', -> doAction 'comment'


    loadMore: ->
      unless @options.app.state.get 'maxResultsReached'
        @unbindScrollEvents()
        @options.app.controller.fetchNextPage().done => @bindScrollEvents()


    disablePointerEvents: ->
      clearTimeout @scrollTimer
      $('body').addClass 'disabled-pointer-events'
      @scrollTimer = setTimeout (-> $('body').removeClass 'disabled-pointer-events'), 250


    onScroll: ->
      @disablePointerEvents()
      if $(window).scrollTop() + $(window).height() >= @ui.loadMore.offset().top
        @loadMoreThrottled()


    scrollToTop: ->
      @$el.scrollParent().scrollTop 0


    scrollToIssue: ->
      selectedIssue = @collection.at @options.app.state.get 'selectedIndex'
      return unless selectedIssue?
      selectedIssueView = @children.findByModel selectedIssue
      viewTop = selectedIssueView.$el.offset().top - TOP_OFFSET
      if selectedIssueView.$el.prev().is('.issues-workspace-list-component')
        viewTop -= COMPONENT_HEIGHT
      viewBottom = selectedIssueView.$el.offset().top + selectedIssueView.$el.outerHeight() + BOTTOM_OFFSET
      windowTop = $(window).scrollTop()
      windowBottom = windowTop + $(window).height()
      if viewTop < windowTop
        $(window).scrollTop viewTop
      if viewBottom > windowBottom
        $(window).scrollTop $(window).scrollTop() - windowBottom + viewBottom


    appendHtml: (compositeView, itemView, index) ->
      $container = this.getItemViewContainer compositeView
      model = @collection.at(index)
      if model?
        prev = @collection.at(index - 1)
        putComponent = !prev?
        if prev?
          fullComponent = [model.get('project'), model.get('component')].join ' '
          fullPrevComponent = [prev.get('project'), prev.get('component')].join ' '
          putComponent = true unless fullComponent == fullPrevComponent
        if putComponent
          $container.append @componentTemplate model.toJSON()
      $container.append itemView.el


    closeChildren: ->
      super
      @$('.issues-workspace-list-component').remove()


