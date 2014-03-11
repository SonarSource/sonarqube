requirejs.config
  baseUrl: "#{baseUrl}/javascripts"

  paths:
    'backbone': 'third-party/backbone'
    'backbone.marionette': 'third-party/backbone.marionette'
    'handlebars': 'third-party/handlebars'
    'jquery.mockjax': 'third-party/jquery.mockjax'

  shim:
    'backbone.marionette':
      deps: ['backbone']
      exports: 'Marionette'
    'backbone':
      exports: 'Backbone'
    'handlebars':
      exports: 'Handlebars'


requirejs [
  'backbone', 'backbone.marionette',

  'coding-rules/layout',
  'coding-rules/router',

  # views
  'coding-rules/views/header-view',
  'coding-rules/views/actions-view',
  'coding-rules/views/filter-bar-view',
  'coding-rules/views/coding-rules-list-view',
  'coding-rules/views/coding-rules-facets-view',

  # filters
  'navigator/filters/base-filters',
  'navigator/filters/choice-filters',
  'navigator/filters/string-filters',
  'coding-rules/views/filters/quality-profile-filter-view',

  'coding-rules/mockjax'
], (
  Backbone, Marionette,

  CodingRulesLayout,
  CodingRulesRouter,

  # views
  CodingRulesHeaderView,
  CodingRulesActionsView,
  CodingRulesFilterBarView,
  CodingRulesListView,
  CodingRulesFacetsView,

  # filters
  BaseFilters,
  ChoiceFilters,
  StringFilterView,
  QualityProfileFilterView
) ->

  # Create a generic error handler for ajax requests
  jQuery.ajaxSetup
    error: (jqXHR) ->
      text = jqXHR.responseText
      errorBox = jQuery('.modal-error')
      if jqXHR.responseJSON?.errors?
        text = _.pluck(jqXHR.responseJSON.errors, 'msg').join '. '
      if errorBox.length > 0
        errorBox.show().text text
      else
        alert text


  # Add html class to mark the page as navigator page
  jQuery('html').addClass('navigator-page coding-rules-page');


  # Create an Application
  App = new Marionette.Application


  App.getQuery =  ->
    @filterBarView.getQuery()


  App.restoreSorting = ->



  App.storeQuery = (query, sorting) ->
    if sorting
      _.extend query,
        sort: sorting.sort
        asc: '' + sorting.asc
    queryString = _.map query, (v, k) -> "#{k}=#{encodeURIComponent(v)}"
    @router.navigate queryString.join('|'), replace: true



  App.fetchList = (firstPage, fetchFacets = true) ->
    query = @getQuery()
    fetchQuery = _.extend { pageIndex: @pageIndex }, query

    if @codingRules.sorting
      _.extend fetchQuery,
          sort: @codingRules.sorting.sort,
          asc: @codingRules.sorting.asc

    unless fetchFacets
      _.extend fetchQuery, facets: false

    @storeQuery query, @codingRules.sorting

    @layout.showSpinner 'resultsRegion'
    @layout.showSpinner 'facetsRegion' if fetchFacets
    jQuery.ajax
      url: "#{baseUrl}/api/codingrules/search"
      data: fetchQuery
    .done (r) =>
      if firstPage
        @codingRules.reset r.codingrules
      else
        @codingRules.add r.codingrules
      @codingRules.paging = r.paging
      @codingRulesListView = new CodingRulesListView
        app: @
        collection: @codingRules
      @layout.resultsRegion.show @codingRulesListView
      @codingRulesListView.selectFirst()

      if fetchFacets
        @facets.reset r.facets
        @codingRulesFacetsView = new CodingRulesFacetsView
          app: @
          collection: @facets
        @layout.facetsRegion.show @codingRulesFacetsView



  App.fetchFirstPage = (fetchFacets = true) ->
    @pageIndex = 1
    App.fetchList true, fetchFacets


  App.fetchNextPage = ->
    if @pageIndex < @codingRules.paging.pages
      @pageIndex++
      App.fetchList false


  App.getActiveQualityProfile = ->
    value = @activeInFilter.get('value')
    if value? && value.length == 1 then value[0] else null


  # Construct layout
  App.addInitializer ->
    @layout = new CodingRulesLayout app: @
    jQuery('body').append @layout.render().el


  # Construct header
  App.addInitializer ->
    @codingRulesHeaderView = new CodingRulesHeaderView app: @
    @layout.headerRegion.show @codingRulesHeaderView


  # Define coding rules
  App.addInitializer ->
    @codingRules = new Backbone.Collection
    @facets = new Backbone.Collection


  # Construct status bar
  App.addInitializer ->
    @codingRulesActionsView = new CodingRulesActionsView
      app: @
      collection: @codingRules
    @layout.actionsRegion.show @codingRulesActionsView


  # Define filters
  App.addInitializer ->
    @filters = new BaseFilters.Filters

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.name_key'
      property: 'searchtext'
      type: StringFilterView
      enabled: true
      optional: false

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.repository'
      property: 'repositories'
      type: ChoiceFilters.ChoiceFilterView
      enabled: true
      optional: false
      choices:
        'checkstyle': 'Checkstyle'
        'common-java': 'Common SonarQube'
        'findbugs': 'FindBugs'
        'pmd': 'PMD'
        'pmd-unit-tests': 'PMD Unit Tests'
        'squid': 'SonarQube'

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.severity'
      property: 'severities'
      type: ChoiceFilters.ChoiceFilterView
      enabled: true
      optional: false
      choices:
        'BLOCKER': t 'severity.BLOCKER'
        'CRITICAL': t 'severity.CRITICAL'
        'MAJOR': t 'severity.MAJOR'
        'MINOR': t 'severity.MINOR'
        'INFO': t 'severity.INFO'
      choiceIcons:
        'BLOCKER': 'severity-blocker'
        'CRITICAL': 'severity-critical'
        'MAJOR': 'severity-major'
        'MINOR': 'severity-minor'
        'INFO': 'severity-info'

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.status'
      property: 'statuses'
      type: ChoiceFilters.ChoiceFilterView
      enabled: true
      optional: false
      choices:
        'BETA': 'Beta'
        'DEPRECATED': 'Deprecated'
        'READY': 'Ready'

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.tag'
      property: 'tags'
      type: ChoiceFilters.ChoiceFilterView
      enabled: true
      optional: false
      choices:
        'brain-overload': 'brain-overload'
        'bug': 'bug'
        'comment': 'comment'
        'convention': 'convention'
        'error-handling': 'error-handling'
        'formatting': 'formatting'
        'java8': 'java8'
        'multithreading': 'multithreading'
        'naming': 'naming'
        'pitfall': 'pitfall'
        'security': 'security'
        'size': 'size'
        'unused': 'unused'
        'unused-code': 'unused-code'

    @activeInFilter = new BaseFilters.Filter
      name: t 'coding_rules.filters.active_in'
      property: 'active_in'
      type: QualityProfileFilterView
      single: true
      enabled: true
      optional: false
    @filters.add @activeInFilter

    @inactiveInFilter = new BaseFilters.Filter
      name: t 'coding_rules.filters.inactive_in'
      property: 'inactive_in'
      type: QualityProfileFilterView
      single: true
      enabled: true
      optional: false
    @filters.add @inactiveInFilter

    @filterBarView = new CodingRulesFilterBarView
      app: @
      collection: @filters,
      extra: sort: '', asc: false
    @layout.filtersRegion.show @filterBarView


  # Start router
  App.addInitializer ->
    @router = new CodingRulesRouter app: @
    Backbone.history.start()


  # Call app before start the application
  appXHR = jQuery.ajax
    url: "#{baseUrl}/api/codingrules/app"

  jQuery.when(appXHR)
  .done (r) ->
      App.appState = new Backbone.Model
      App.state = new Backbone.Model
      App.qualityProfiles = r.qualityprofiles
      window.messages = r.messages

      # Remove the initial spinner
      jQuery('#coding-rules-page-loader').remove()

      # Start the application
      App.start()
