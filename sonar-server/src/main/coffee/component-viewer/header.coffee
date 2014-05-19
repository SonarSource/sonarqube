define [
  'backbone.marionette'
  'templates/component-viewer'
  'common/handlebars-extensions'
], (
  Marionette
  Templates
) ->

  $ = jQuery


  class HeaderView extends Marionette.ItemView
    template: Templates['header']


    ui:
      expandLinks: '.component-viewer-header-measures-expand'
      expandedBars: '.component-viewer-header-expanded-bar'


    events:
      'click @ui.expandLinks': 'showExpandedBar'

      'click .js-toggle-issues': 'toggleIssues'
      'click .js-toggle-coverage': 'toggleCoverage'
      'click .js-toggle-duplications': 'toggleDuplications'
      'click .js-toggle-scm': 'toggleSCM'

      'click .js-issues-bulk-change': 'issuesBulkChange'

      'click .js-filter-current-issue': 'filterByCurrentIssue'
      'click .js-filter-all-issues': 'filterByAllIssues'
      'click .js-filter-rule': 'filterByRule'
      'click .js-filter-resolved-issues': 'filterByResolvedIssues'
      'click .js-filter-unresolved-issues': 'filterByUnresolvedIssues'
      'click .js-filter-false-positive-issues': 'filterByFalsePositiveIssues'
      'click .js-filter-BLOCKER-issues': 'filterByBlockerIssues'
      'click .js-filter-CRITICAL-issues': 'filterByCriticalIssues'
      'click .js-filter-MAJOR-issues': 'filterByMajorIssues'
      'click .js-filter-MINOR-issues': 'filterByMinorIssues'
      'click .js-filter-INFO-issues': 'filterByInfoIssues'

      'click .js-filter-lines-to-cover': 'filterByLinesToCover'
      'click .js-filter-covered-lines': 'filterByCoveredLines'
      'click .js-filter-uncovered-lines': 'filterByUncoveredLines'
      'click .js-filter-branches-to-cover': 'filterByBranchesToCover'
      'click .js-filter-covered-branches': 'filterByCoveredBranches'
      'click .js-filter-uncovered-branches': 'filterByUncoveredBranches'
      'click .js-filter-lines-to-cover-it': 'filterByLinesToCoverIT'
      'click .js-filter-covered-lines-it': 'filterByCoveredLinesIT'
      'click .js-filter-uncovered-lines-it': 'filterByUncoveredLinesIT'
      'click .js-filter-branches-to-cover-it': 'filterByBranchesToCoverIT'
      'click .js-filter-covered-branches-it': 'filterByCoveredBranchesIT'
      'click .js-filter-uncovered-branches-it': 'filterByUncoveredBranchesIT'


    initialize: (options) ->
      options.main.settings.on 'change', => @changeSettings()


    onRender: ->
      @delegateEvents()


    showExpandedBar: (e) ->
      el = $(e.currentTarget)
      if el.is '.active'
        @ui.expandLinks.removeClass 'active'
        @ui.expandedBars.hide()
      else
        @ui.expandLinks.removeClass 'active'
        el.addClass 'active'
        scope = el.data 'scope'
        @ui.expandedBars.hide()
        if scope
          unless @options.main.component.has 'msr'
            @options.main.requestMeasures(@options.main.key).done =>
              @render()
              @ui.expandLinks.filter("[data-scope=#{scope}]").addClass 'active'
              @ui.expandedBars.filter("[data-scope=#{scope}]").show()
          else
            @ui.expandedBars.filter("[data-scope=#{scope}]").show()


    changeSettings: ->
      @$('.js-toggle-issues').toggleClass 'active', @options.main.settings.get 'issues'
      @$('.js-toggle-coverage').toggleClass 'active', @options.main.settings.get 'coverage'
      @$('.js-toggle-duplications').toggleClass 'active', @options.main.settings.get 'duplications'
      @$('.js-toggle-scm').toggleClass 'active', @options.main.settings.get 'scm'


    toggleSetting: (e, show, hide) ->
      @showBlocks = []
      active = $(e.currentTarget).is '.active'
      if active then hide.call @options.main else show.call @options.main


    toggleIssues: (e) -> @toggleSetting e, @options.main.showIssues, @options.main.hideIssues
    toggleCoverage: (e) -> @toggleSetting e, @options.main.showCoverage, @options.main.hideCoverage
    toggleDuplications: (e) -> @toggleSetting e, @options.main.showDuplications, @options.main.hideDuplications
    toggleSCM: (e) -> @toggleSetting e, @options.main.showSCM, @options.main.hideSCM
    toggleWorkspace: (e) -> @toggleSetting e, @options.main.showWorkspace, @options.main.hideWorkspace


    issuesBulkChange: ->
      issues = @model.get('activeIssues')?.map (issue) -> issue.key
      if issues.length > 0
        url = "#{baseUrl}/issues/bulk_change_form?issues=#{issues.join()}"
        openModalWindow url, {}


    filterLines: (e, methodName, extra) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      method = @options.main[methodName]
      method.call @options.main, extra


    # Issues
    filterByCurrentIssue: (e) -> @filterLines e, 'filterByCurrentIssue'
    filterByAllIssues: (e) -> @filterLines e, 'filterByAllIssues'
    filterByResolvedIssues: (e) -> @filterLines e, 'filterByResolvedIssues'
    filterByUnresolvedIssues: (e) -> @filterLines e, 'filterByUnresolvedIssues'
    filterByFalsePositiveIssues: (e) -> @filterLines e, 'filterByFalsePositiveIssues'

    filterByRule: (e) -> @filterLines e, 'filterByRule', $(e.currentTarget).data 'rule'

    filterByBlockerIssues: (e) -> @filterLines e, 'filterByBlockerIssues'
    filterByCriticalIssues: (e) -> @filterLines e, 'filterByCriticalIssues'
    filterByMajorIssues: (e) -> @filterLines e, 'filterByMajorIssues'
    filterByMinorIssues: (e) -> @filterLines e, 'filterByMinorIssues'
    filterByInfoIssues: (e) -> @filterLines e, 'filterByInfoIssues'


    # Coverage
    filterByLinesToCover: (e) -> @filterLines e, 'filterByLinesToCover'
    filterByCoveredLines: (e) -> @filterLines e, 'filterByCoveredLines'
    filterByUncoveredLines: (e) -> @filterLines e, 'filterByUncoveredLines'
    filterByBranchesToCover: (e) -> @filterLines e, 'filterByBranchesToCover'
    filterByCoveredBranches: (e) -> @filterLines e, 'filterByCoveredBranches'
    filterByUncoveredBranches: (e) -> @filterLines e, 'filterByUncoveredBranches'

    filterByLinesToCoverIT: (e) -> @filterLines e, 'filterByLinesToCoverIT'
    filterByCoveredLinesIT: (e) -> @filterLines e, 'filterByCoveredLinesIT'
    filterByUncoveredLinesIT: (e) -> @filterLines e, 'filterByUncoveredLinesIT'
    filterByBranchesToCoverIT: (e) -> @filterLines e, 'filterByBranchesToCoverIT'
    filterByCoveredBranchesIT: (e) -> @filterLines e, 'filterByCoveredBranchesIT'
    filterByUncoveredBranchesIT: (e) -> @filterLines e, 'filterByUncoveredBranchesIT'


    serializeData: ->
      component = @options.main.component.toJSON()
      if component.measures
        component.measures.maxIssues = Math.max(
          component.measures.fBlockerIssues || 0
          component.measures.fCriticalIssues || 0
          component.measures.fMajorIssues || 0
          component.measures.fMinorIssues || 0
          component.measures.fInfoIssues || 0
        )

      if component.severities
        order = ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        component.severities = _.sortBy component.severities, (s) -> order.indexOf s[0]


      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: component