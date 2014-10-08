define [
  'backbone.marionette'
  'monitoring/views/report-view'
], (
  Marionette
  ReportView
) ->

  class extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: ReportView


    itemViewOptions: ->
      listView: @, app: @options.app
