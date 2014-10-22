define [
  'backbone.marionette'
  'templates/analysis-reports'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['analysis-reports-report']


    onRender: ->
      status = @model.get 'status'
      @$el.addClass 'analysis-reports-report-pending' if status is 'PENDING'
      @$el.addClass 'analysis-reports-report-working' if status is 'WORKING'
      @$el.addClass 'analysis-reports-report-done' if status is 'SUCCESS'
      @$el.addClass 'analysis-reports-report-failed' if status is 'FAIL'
