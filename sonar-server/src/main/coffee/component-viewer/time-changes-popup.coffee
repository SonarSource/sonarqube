define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/popup'
], (
  Marionette
  Templates
  Popup
) ->

  $ = jQuery


  class TimeChangesPopupView extends Popup
    template: Templates['time-changes-popup']


    events:
      'click a[data-period]': 'enablePeriod'


    enablePeriod: (e) ->
      period = $(e.currentTarget).data 'period'
      @options.main.enablePeriod period


    serializeData: ->
      component: @options.main.component.toJSON()
      periods: @options.main.periods.toJSON()
