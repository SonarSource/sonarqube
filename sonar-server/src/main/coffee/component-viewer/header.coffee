define [
  'backbone.marionette'
  'templates/component-viewer'

  'component-viewer/header/basic-header'
  'component-viewer/header/issues-header'
  'component-viewer/header/coverage-header'
  'component-viewer/header/duplications-header'
  'component-viewer/header/scm-header'
  'component-viewer/header/tests-header'
  'component-viewer/header/more-actions'

  'common/handlebars-extensions'
], (
  Marionette
  Templates

  BasicHeaderView
  IssuesHeaderView
  CoverageHeaderView
  DuplicationsHeaderView
  SCMHeaderView
  TestsHeaderView
  MoreActionsView
) ->

  $ = jQuery

  API_FAVORITE = "#{baseUrl}/api/favourites"
  API_EXTENSION = "#{baseUrl}/resource/extension"
  BARS = [
    { scope: 'basic', view: BasicHeaderView }
    { scope: 'issues', view: IssuesHeaderView }
    { scope: 'coverage', view: CoverageHeaderView }
    { scope: 'duplications', view: DuplicationsHeaderView }
    { scope: 'scm', view: SCMHeaderView }
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
      'click .js-actions': 'showMoreActions'
      'click .js-extension-close': 'closeExtension'
      'click .js-permalink': 'getPermalink'
      'click @ui.expandLinks': 'showExpandedBar'
      'click .js-toggle-issues': 'toggleIssues'
      'click .js-toggle-coverage': 'toggleCoverage'
      'click .js-toggle-duplications': 'toggleDuplications'
      'click .js-toggle-scm': 'toggleSCM'


    initialize: (options) ->
      options.main.settings.on 'change', => @changeSettings()
      @state = options.main.state
      @component = options.main.component
      @settings = options.main.component


    onRender: ->
      @delegateEvents()
      activeHeaderTab = @state.get 'activeHeaderTab'
      activeHeaderItem = @state.get 'activeHeaderItem'
      if activeHeaderTab
        if _.findWhere(BARS, scope: activeHeaderTab)?
          @enableBar(activeHeaderTab).done =>
            if activeHeaderItem
              @enableBarItem activeHeaderItem, @silentUpdate
        else
          @showExtension activeHeaderTab
      @silentUpdate = false


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


    showMoreActions: (e) ->
      e.stopPropagation()
      $('body').click()
      view = new MoreActionsView main: @options.main
      view.render().$el.appendTo @$el


    showExtension: (key) ->
      bar = @ui.expandedBar
      bar.html('<i class="spinner spinner-margin"></i>').addClass 'active'
      @ui.expandLinks.removeClass 'active'
      $.get API_EXTENSION, id: @options.main.component.get('key'), tab: key, (r) =>
        bar.html r


    closeExtension: (e) ->
      e.preventDefault()
      @ui.expandedBar.html('').removeClass 'active'


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
      @ui.expandedBar.html('<i class="spinner spinner-margin"></i>').addClass 'active'
      requests = []
      unless @state.get 'hasMeasures'
        requests.push @options.main.requestMeasures @options.main.key
      if @component.get('isUnitTest') && !@state.get('hasTests')
        requests.push @options.main.requestTests @options.main.key
      $.when.apply($, requests).done =>
        @state.set 'activeHeaderTab', scope
        bar = _.findWhere BARS, scope: scope
        @barRegion.show new bar.view
          main: @options.main, state: @state, component: @component, settings: @settings, source: @model, header: @
        @ui.expandedBar.addClass 'active'
        @ui.expandLinks.filter("[data-scope=#{scope}]").addClass 'active'
        activeHeaderItem = @state.get 'activeHeaderItem'
        if activeHeaderItem
          @$(activeHeaderItem).addClass 'active'


    enableBarItem: (item, silent = false) ->
      $item = @$(item)
      if $item.length > 0
        if silent then @$(item).addClass('active') else  @$(item).click()
      else
        @options.main.hideAllLines()


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


    showTimeChangesSpinner: ->
      @$('.component-viewer-header-time-changes').html '<i class="spinner spinner-margin"></i>'


    filterLines: (e, methodName, extra) ->
      @$('.component-viewer-header-expanded-bar-section-list .active').removeClass 'active'
      $(e.currentTarget).addClass 'active'
      method = @options.main[methodName]
      method.call @options.main, extra


    serializeShowBlocks: ->
      blocks = @options.main.sourceView.showBlocks.map (b) -> "#{b.from},#{b.to}"
      blocks.join ';'


    getPermalink: ->
      params = []
      params.push key: 'component', value: @options.main.component.get 'key'
      settings = []
      _.map @options.main.settings.toJSON(), (v, k) -> settings.push k if v
      params.push key: 'settings', value: settings.join ','
      params.push key: 'blocks', value: @serializeShowBlocks()
      activeHeaderTab = @state.get 'activeHeaderTab'
      if activeHeaderTab
        params.push key: 'tab', value: activeHeaderTab
      activeHeaderItem = @state.get 'activeHeaderItem'
      if activeHeaderItem
        params.push key: 'item', value: activeHeaderItem
      highlightedLine = @options.main.sourceView.highlightedLine
      period = @state.get 'period'
      if period?
        params.push key: 'period', value: period.get 'key'
      if @options.main.currentIssue
        params.push key: 'currentIssue', value: @options.main.currentIssue
      if highlightedLine
        params.push key: 'line', value: highlightedLine
      hash = params.map((d) -> "#{d.key}=#{encodeURIComponent(d.value)}" ).join '&'

      windowParams = 'resizable=1,scrollbars=1,status=1'
      window.open "#{baseUrl}/component/index##{hash}", @options.main.component.get('name'), windowParams



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
