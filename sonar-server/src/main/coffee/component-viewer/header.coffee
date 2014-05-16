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


    filterByCoverage: (e, method) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      _.result @options.main, method


    filterByLinesToCover: (e) -> @filterByCoverage e, 'filterByLinesToCover'
    filterByCoveredLines: (e) -> @filterByCoverage e, 'filterByCoveredLines'
    filterByUncoveredLines: (e) -> @filterByCoverage e, 'filterByUncoveredLines'
    filterByBranchesToCover: (e) -> @filterByCoverage e, 'filterByBranchesToCover'
    filterByCoveredBranches: (e) -> @filterByCoverage e, 'filterByCoveredBranches'
    filterByUncoveredBranches: (e) -> @filterByCoverage e, 'filterByUncoveredBranches'

    filterByLinesToCoverIT: (e) -> @filterByCoverage e, 'filterByLinesToCoverIT'
    filterByCoveredLinesIT: (e) -> @filterByCoverage e, 'filterByCoveredLinesIT'
    filterByUncoveredLinesIT: (e) -> @filterByCoverage e, 'filterByUncoveredLinesIT'
    filterByBranchesToCoverIT: (e) -> @filterByCoverage e, 'filterByBranchesToCoverIT'
    filterByCoveredBranchesIT: (e) -> @filterByCoverage e, 'filterByCoveredBranchesIT'
    filterByUncoveredBranchesIT: (e) -> @filterByCoverage e, 'filterByUncoveredBranchesIT'


    serializeData: ->
      component = @options.main.component.toJSON()
      if component.measures
        component.measures.max_issues = Math.max(
          component.measures.blocker_issues
          component.measures.critical_issues
          component.measures.major_issues
          component.measures.minor_issues
          component.measures.info_issues
        )

      settings: @options.main.settings.toJSON()
      showSettings: @showSettings
      component: component