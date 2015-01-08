define [
  'analysis-reports/views/report-view'
  'analysis-reports/views/reports-empty-view'
], (
  ReportView
  EmptyView
) ->

  $ = jQuery


  class extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: ReportView
    emptyView: EmptyView


    itemViewOptions: ->
      listView: @, app: @options.app


    initialize: ->
      @loadMoreThrottled = _.throttle @loadMore, 200


    onClose: ->
      @unbindScrollEvents()


    bindScrollEvents: ->
      $(window).on 'scroll', (=> @loadMoreThrottled())


    unbindScrollEvents: ->
      $(window).off 'scroll'


    loadMore: ->
      lastItem = this.children.findByIndex(@collection.length - 1)
      if $(window).scrollTop() + $(window).outerHeight() >= lastItem.$el.offset().top - 40
        @unbindScrollEvents()
        @options.app.fetchNextPage().done =>
          @bindScrollEvents() unless @collection.paging.maxResultsReached
