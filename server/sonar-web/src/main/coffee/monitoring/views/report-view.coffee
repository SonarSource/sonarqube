define [
  'backbone.marionette'
  'templates/monitoring'
], (
  Marionette
  Templates
) ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['monitoring-report']


    onRender: ->
      status = @model.get 'status'
      @$el.addClass 'monitoring-report-pending' if status is 'PENDING'
      @$el.addClass 'monitoring-report-working' if status is 'WORKING'
      @$el.addClass 'monitoring-report-done' if status is 'DONE'
