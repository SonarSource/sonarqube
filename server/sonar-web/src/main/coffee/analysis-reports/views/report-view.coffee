define [
  'templates/analysis-reports'
], ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['analysis-reports-report']


    onRender: ->
      status = @model.get 'status'
      @$el.addClass 'analysis-reports-report-pending' if status is 'PENDING'
      @$el.addClass 'analysis-reports-report-working' if status is 'WORKING'
      @$el.addClass 'analysis-reports-report-done' if status is 'SUCCESS'
      @$el.addClass 'analysis-reports-report-failed' if status is 'FAIL'


    serializeData: ->
      duration = null
      if @model.has('startedAt') && @model.has('finishedAt')
        startedAtMoment = moment @model.get 'startedAt'
        finishedAtMoment = moment @model.get 'finishedAt'
        duration = finishedAtMoment.diff startedAtMoment
        duration =
          seconds: Math.floor (duration / 1000) % 60
          minutes: Math.floor (duration / (1000 * 60)) % 60
          hours: Math.floor (duration / (1000 * 60 * 60)) % 24
      _.extend super,
        duration: duration
