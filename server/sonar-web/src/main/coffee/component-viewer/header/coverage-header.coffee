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
    template: Templates['cw-coverage-header']


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

      'click .js-filter-lines-to-cover-overall': 'filterByLinesToCoverOverall'
      'click .js-filter-uncovered-lines-overall': 'filterByUncoveredLinesOverall'
      'click .js-filter-branches-to-cover-overall': 'filterByBranchesToCoverOverall'
      'click .js-filter-uncovered-branches-overall': 'filterByUncoveredBranchesOverall'


    coverageTimeChanges: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new TimeChangesPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
        bottom: true
      popup.render()
      popup.on 'change', (period) => @main.enablePeriod period, '.js-filter-lines-to-cover'


    filterByLinesToCover: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByLinesToCover'
      @state.set 'activeHeaderItem', '.js-filter-lines-to-cover'


    filterByUncoveredLines: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredLines'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-lines'


    filterByBranchesToCover: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByBranchesToCover'
      @state.set 'activeHeaderItem', '.js-filter-branches-to-cover'


    filterByUncoveredBranches: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredBranches'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-branches'


    filterByLinesToCoverIT: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByLinesToCoverIT'
      @state.set 'activeHeaderItem', '.js-filter-lines-to-cover-it'


    filterByUncoveredLinesIT: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredLinesIT'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-lines-it'


    filterByBranchesToCoverIT: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByBranchesToCoverIT'
      @state.set 'activeHeaderItem', '.js-filter-branches-to-cover-it'


    filterByUncoveredBranchesIT: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredBranchesIT'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-branches-it'


    filterByLinesToCoverOverall: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByLinesToCoverOverall'
      @state.set 'activeHeaderItem', '.js-filter-lines-to-cover-overall'


    filterByUncoveredLinesOverall: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredLinesOverall'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-lines-overall'


    filterByBranchesToCoverOverall: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByBranchesToCoverOverall'
      @state.set 'activeHeaderItem', '.js-filter-branches-to-cover-overall'


    filterByUncoveredBranchesOverall: (e) ->
      return @header.unsetFilter() if $(e.currentTarget).is('.active')
      @header.filterLines e, 'filterByUncoveredBranchesOverall'
      @state.set 'activeHeaderItem', '.js-filter-uncovered-branches-overall'


    serializeData: ->
      _.extend super, period: @state.get('period')?.toJSON()
