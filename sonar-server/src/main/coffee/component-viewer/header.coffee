define [
  'backbone.marionette'
  'templates/component-viewer'

  'component-viewer/header/basic-header'
  'component-viewer/header/issues-header'
  'component-viewer/header/coverage-header'
  'component-viewer/header/duplications-header'
  'component-viewer/header/tests-header'

  'component-viewer/time-changes-popup'

  'common/handlebars-extensions'
], (
  Marionette
  Templates

  BasicHeaderView
  IssuesHeaderView
  CoverageHeaderView
  DuplicationsHeaderView
  TestsHeaderView

  TimeChangesPopupView
) ->

  $ = jQuery

  API_FAVORITE = "#{baseUrl}/api/favourites"
  BARS = [
    { scope: 'basic', view: BasicHeaderView }
    { scope: 'issues', view: IssuesHeaderView }
    { scope: 'coverage', view: CoverageHeaderView }
    { scope: 'duplications', view: DuplicationsHeaderView }
    { scope: 'tests', view: TestsHeaderView }
  ]


  class extends Marionette.Layout
    template: Templates['header']


    regions:
      barRegion: '.component-viewer-header-expanded-bar'


    ui:
      expandLinks: '.component-viewer-header-measures-expand'
      expandedBar: '.component-viewer-header-expanded-bar'
      spinnerBar: '.component-viewer-header-expanded-bar[data-scope=spinner]'
      unitTests: '.js-unit-test'


    events:
      'click .js-favorite': 'toggleFavorite'

      'click @ui.expandLinks': 'showExpandedBar'

      'click .js-toggle-issues': 'toggleIssues'
      'click .js-toggle-coverage': 'toggleCoverage'
      'click .js-toggle-duplications': 'toggleDuplications'
      'click .js-toggle-scm': 'toggleSCM'

      'click .component-viewer-header-time-changes a': 'showTimeChangesPopup'


    initialize: (options) ->
      options.main.settings.on 'change', => @changeSettings()
      @state = options.main.state
      @component = options.main.component
      @settings = options.main.component


    onRender: ->
      @delegateEvents()


    toggleFavorite: ->
      component = @component
      if component.get 'fav'
        $.ajax
          url: "#{API_FAVORITE}/#{component.get 'key'}"
          type: 'DELETE'
        .done =>
          component.set 'fav', false
          @render()
      else
        $.ajax
          url: API_FAVORITE
          type: 'POST'
          data: key: component.get 'key'
        .done =>
          component.set 'fav', true
          @render()


    showBarSpinner: ->
      @ui.spinnerBar.addClass 'active'


    hideBarSpinner: ->
      @ui.spinnerBar.removeClass 'active'


    resetBars: ->
      @state.set 'activeHeaderTab', null
      @ui.expandLinks.removeClass 'active'
      @ui.expandedBar.removeClass 'active'
      @barRegion.reset()


    enableBar: (scope) ->
      @ui.expandedBar.addClass 'active'
      requests = []
      unless @state.get 'hasMeasures'
        requests.push @options.main.requestMeasures @options.main.key
      if @component.get('isUnitTest') && !@state.get('hasTests')
        requests.push @options.main.requestTests @options.main.key
      $.when.apply($, requests).done =>
        @state.set 'activeHeaderTab', scope
        bar = _.findWhere BARS, scope: scope
        @barRegion.show new bar.view
          state: @state, component: @component, settings: @settings, source: @model, header: @
        @ui.expandLinks.filter("[data-scope=#{scope}]").addClass 'active'


    showExpandedBar: (e) ->
      el = $(e.currentTarget)
      active = el.is '.active'
      @resetBars()
      unless active
        el.addClass 'active'
        scope = el.data 'scope'
        @enableBar scope


    changeSettings: ->
      @$('.js-toggle-issues').toggleClass 'active', @options.main.settings.get 'issues'
      @$('.js-toggle-coverage').toggleClass 'active', @options.main.settings.get 'coverage'
      @$('.js-toggle-duplications').toggleClass 'active', @options.main.settings.get 'duplications'
      @$('.js-toggle-scm').toggleClass 'active', @options.main.settings.get 'scm'


    toggleSetting: (e, show, hide) ->
      @showBlocks = []
      active = $(e.currentTarget).is '.active'
      if active
        hide.call @options.main, true
      else
        show.call @options.main, true


    toggleIssues: (e) -> @toggleSetting e, @options.main.showIssues, @options.main.hideIssues
    toggleCoverage: (e) -> @toggleSetting e, @options.main.showCoverage, @options.main.hideCoverage
    toggleDuplications: (e) -> @toggleSetting e, @options.main.showDuplications, @options.main.hideDuplications
    toggleSCM: (e) -> @toggleSetting e, @options.main.showSCM, @options.main.hideSCM
    toggleWorkspace: (e) -> @toggleSetting e, @options.main.showWorkspace, @options.main.hideWorkspace


    showTimeChangesPopup: (e) ->
      e.stopPropagation()
      $('body').click()
      popup = new TimeChangesPopupView
        triggerEl: $(e.currentTarget)
        main: @options.main
      popup.render()


    filterLines: (e, methodName, extra) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      method = @options.main[methodName]
      method.call @options.main, extra


    serializeData: ->
      component = @component.toJSON()
      if component.measures
        component.measures.maxIssues = Math.max(
          component.measures.fBlockerIssues || 0
          component.measures.fCriticalIssues || 0
          component.measures.fMajorIssues || 0
          component.measures.fMinorIssues || 0
          component.measures.fInfoIssues || 0
        )

      settings: @options.main.settings.toJSON()
      state: @state.toJSON()
      showSettings: @showSettings
      component: component
      currentIssue: @options.main.currentIssue
      period: @options.main.period?.toJSON()