define [
  'backbone.marionette'
  'templates/component-viewer'
  'component-viewer/header/base-header'
], (
  Marionette
  Templates
  BaseHeaderView
) ->

  $ = jQuery


  class extends BaseHeaderView
    template: Templates['issues-header']


    events:
      'click .js-issues-bulk-change': 'issuesBulkChange'

      'click .js-filter-current-issue': 'filterByCurrentIssue'
      'click .js-filter-all-issues': 'filterByAllIssues'
      'click .js-filter-rule': 'filterByRule'
      'click .js-filter-fixed-issues': 'filterByFixedIssues'
      'click .js-filter-unresolved-issues': 'filterByUnresolvedIssues'
      'click .js-filter-false-positive-issues': 'filterByFalsePositiveIssues'
      'click .js-filter-BLOCKER-issues': 'filterByBlockerIssues'
      'click .js-filter-CRITICAL-issues': 'filterByCriticalIssues'
      'click .js-filter-MAJOR-issues': 'filterByMajorIssues'
      'click .js-filter-MINOR-issues': 'filterByMinorIssues'
      'click .js-filter-INFO-issues': 'filterByInfoIssues'


    issuesBulkChange: ->
      issues = @source.get('activeIssues')?.map (issue) -> issue.key
      if issues.length > 0
        url = "#{baseUrl}/issues/bulk_change_form?issues=#{issues.join()}"
        openModalWindow url, {}


    filterByCurrentIssue: (e) -> @header.filterLines e, 'filterByCurrentIssue'
    filterByAllIssues: (e) -> @header.filterLines e, 'filterByAllIssues'
    filterByFixedIssues: (e) -> @header.filterLines e, 'filterByFixedIssues'
    filterByUnresolvedIssues: (e) -> @header.filterLines e, 'filterByUnresolvedIssues'
    filterByFalsePositiveIssues: (e) -> @header.filterLines e, 'filterByFalsePositiveIssues'

    filterByRule: (e) -> @header.filterLines e, 'filterByRule', $(e.currentTarget).data 'rule'

    filterByBlockerIssues: (e) -> @header.filterLines e, 'filterByBlockerIssues'
    filterByCriticalIssues: (e) -> @header.filterLines e, 'filterByCriticalIssues'
    filterByMajorIssues: (e) -> @header.filterLines e, 'filterByMajorIssues'
    filterByMinorIssues: (e) -> @header.filterLines e, 'filterByMinorIssues'
    filterByInfoIssues: (e) -> @header.filterLines e, 'filterByInfoIssues'