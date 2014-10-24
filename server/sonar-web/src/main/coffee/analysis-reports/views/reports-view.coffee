define [
  'backbone.marionette'
  'analysis-reports/views/report-view'
  'analysis-reports/views/reports-empty-view'
], (
  Marionette
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
      $('.navigator-results').on 'scroll', (=> @loadMoreThrottled())


    unbindScrollEvents: ->
      $('.navigator-results').off 'scroll'


    loadMore: ->
      if $('.navigator-results').scrollTop() + $('.navigator-results').outerHeight() >= $('.navigator-results-list').outerHeight() - 40
        @unbindScrollEvents()
        @options.app.fetchNextPage().done =>
          @bindScrollEvents() unless @collection.paging.maxResultsReached
