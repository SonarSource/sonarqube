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

      'click .js-filter-line-to-cover': 'filterByLinesToCover'
      'click .js-filter-uncovered-lines': 'filterByUncoveredLines'


    initialize: (options) ->
#      @listenTo options.main.settings, 'change', @changeSettings
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


    toggleIssues: (e) ->
      @toggleSetting e, @options.main.showIssues, @options.main.hideIssues


    toggleCoverage: (e) ->
      @toggleSetting e, @options.main.showCoverage, @options.main.hideCoverage


    toggleDuplications: (e) ->
      @toggleSetting e, @options.main.showDuplications, @options.main.hideDuplications


    toggleSCM: (e) ->
      @toggleSetting e, @options.main.showSCM, @options.main.hideSCM


    toggleWorkspace: (e) ->
      @toggleSetting e, @options.main.showWorkspace, @options.main.hideWorkspace


    filterByLinesToCover: (e) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      @options.main.filterLinesByLinesToCover()


    filterByUncoveredLines: (e) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      @options.main.filterLinesByUncoveredLines()


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