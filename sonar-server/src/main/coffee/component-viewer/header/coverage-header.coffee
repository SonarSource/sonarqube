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
    template: Templates['coverage-header']


    events:
      'click .js-coverage-time-changes': 'coverageTimeChanges'

      'click .js-filter-lines-to-cover': 'filterByLinesToCover'
      'click .js-filter-uncovered-lines': 'filterByUncoveredLines'
      'click .js-filter-branches-to-cover': 'filterByBranchesToCover'
      'click .js-filter-uncovered-branches': 'filterByUncoveredBranches'
      'click .js-filter-lines-to-cover-it': 'filterByLinesToCoverIT'
      'click .js-filter-uncovered-lines-it': 'filterByUncoveredLinesIT'
      'click .js-filter-branches-to-cover-it': 'filterByBranchesToCoverIT'
      'click .js-filter-uncovered-branches-it': 'filterByUncoveredBranchesIT'


    coverageTimeChanges: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new TimeChangesPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        bottom: true
      popup.render()
      popup.on 'change', (period) => @main.enableSCMPeriod period


    filterByLinesToCover: (e) ->
      @header.filterLines e, 'filterByLinesToCover'
      @state.set 'activeHeaderItem', '.js-filter-lines-to-cover'


    filterByUncoveredLines: (e) ->
      @header.filterLines e, 'filterByUncoveredLines'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-lines'


    filterByBranchesToCover: (e) ->
      @header.filterLines e, 'filterByBranchesToCover'
      @state.set 'activeHeaderItem', '.js-filter-branches-to-cover'


    filterByUncoveredBranches: (e) ->
      @header.filterLines e, 'filterByUncoveredBranches'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-branches'


    filterByLinesToCoverIT: (e) ->
      @header.filterLines e, 'filterByLinesToCoverIT'
      @state.set 'activeHeaderItem', '.js-filter-lines-to-cover-it'


    filterByUncoveredLinesIT: (e) ->
      @header.filterLines e, 'filterByUncoveredLinesIT'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-lines-it'


    filterByBranchesToCoverIT: (e) ->
      @header.filterLines e, 'filterByBranchesToCoverIT'
      @state.set 'activeHeaderItem', '.js-filter-branches-to-cover-it'


    filterByUncoveredBranchesIT: (e) ->
      @header.filterLines e, 'filterByUncoveredBranchesIT'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-branches-it'


    serializeData: ->
      _.extend super, period: @state.get('period')?.toJSON()