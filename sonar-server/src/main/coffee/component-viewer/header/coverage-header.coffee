define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
], (
  Marionette
  Templates
  BaseHeaderView
) ->


  class extends BaseHeaderView
    template: Templates['coverage-header']


    events:
      'click .js-filter-lines-to-cover': 'filterByLinesToCover'
      'click .js-filter-uncovered-lines': 'filterByUncoveredLines'
      'click .js-filter-branches-to-cover': 'filterByBranchesToCover'
      'click .js-filter-uncovered-branches': 'filterByUncoveredBranches'
      'click .js-filter-lines-to-cover-it': 'filterByLinesToCoverIT'
      'click .js-filter-uncovered-lines-it': 'filterByUncoveredLinesIT'
      'click .js-filter-branches-to-cover-it': 'filterByBranchesToCoverIT'
      'click .js-filter-uncovered-branches-it': 'filterByUncoveredBranchesIT'


    filterByLinesToCover: (e) -> @header.filterLines e, 'filterByLinesToCover'
    filterByCoveredLines: (e) -> @header.filterLines e, 'filterByCoveredLines'
    filterByUncoveredLines: (e) -> @header.filterLines e, 'filterByUncoveredLines'
    filterByBranchesToCover: (e) -> @header.filterLines e, 'filterByBranchesToCover'
    filterByCoveredBranches: (e) -> @header.filterLines e, 'filterByCoveredBranches'
    filterByUncoveredBranches: (e) -> @header.filterLines e, 'filterByUncoveredBranches'

    filterByLinesToCoverIT: (e) -> @header.filterLines e, 'filterByLinesToCoverIT'
    filterByCoveredLinesIT: (e) -> @header.filterLines e, 'filterByCoveredLinesIT'
    filterByUncoveredLinesIT: (e) -> @header.filterLines e, 'filterByUncoveredLinesIT'
    filterByBranchesToCoverIT: (e) -> @header.filterLines e, 'filterByBranchesToCoverIT'
    filterByCoveredBranchesIT: (e) -> @header.filterLines e, 'filterByCoveredBranchesIT'
    filterByUncoveredBranchesIT: (e) -> @header.filterLines e, 'filterByUncoveredBranchesIT'