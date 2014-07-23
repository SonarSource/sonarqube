define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
  'component-viewer/time-changes-popup'
], (
  Marionette
  Templates
  BaseHeaderView
  TimeChangesPopupView
) ->

  $ = jQuery


  class extends BaseHeaderView
    template: Templates['scm-header']


    events:
      'click .js-scm-time-changes': 'scmTimeChanges'

      'click .js-filter-modified-lines': 'filterByModifiedLines'


    scmTimeChanges: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new TimeChangesPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        bottom: true
      popup.render()
      popup.on 'change', (period) => @main.enablePeriod period


    filterByModifiedLines: (e) ->
      @header.filterLines e, 'filterByModifiedLines'
      @state.set 'activeHeaderItem', '.js-filter-modified-lines'


    serializeData: ->
      _.extend super, period: @state.get('period')?.toJSON()
