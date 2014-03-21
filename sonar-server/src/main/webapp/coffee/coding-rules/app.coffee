requirejs.config
  baseUrl: "#{baseUrl}/js"

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
  'coding-rules/views/coding-rules-bulk-change-view',
  'coding-rules/views/coding-rules-quality-profile-activation-view',
  'coding-rules/views/coding-rules-bulk-change-dropdown-view'

  # filters
  'navigator/filters/base-filters',
  'navigator/filters/choice-filters',
  'navigator/filters/string-filters',
  'navigator/filters/date-filter-view',
  'coding-rules/views/filters/quality-profile-filter-view',
  'coding-rules/views/filters/inheritance-filter-view',

  'coding-rules/mockjax',
  'common/handlebars-extensions'
], (
  Backbone, Marionette,

  CodingRulesLayout,
  CodingRulesRouter,

  # views
  CodingRulesHeaderView,
  CodingRulesActionsView,
  CodingRulesFilterBarView,
  CodingRulesListView,
  CodingRulesBulkChangeView,
  CodingRulesQualityProfileActivationView,
  CodingRulesBulkChangeDropdownView

  # filters
  BaseFilters,
  ChoiceFilters,
  StringFilterView,
  DateFilterView,
  QualityProfileFilterView,
  InheritanceFilterView
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



  App.fetchList = (firstPage) ->
    query = @getQuery()
    fetchQuery = _.extend { pageIndex: @pageIndex }, query

    if @codingRules.sorting
      _.extend fetchQuery,
          sort: @codingRules.sorting.sort,
          asc: @codingRules.sorting.asc

    @storeQuery query, @codingRules.sorting

    @layout.showSpinner 'resultsRegion'
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



  App.fetchFirstPage = ->
    @pageIndex = 1
    App.fetchList true


  App.fetchNextPage = ->
    if @pageIndex < @codingRules.paging.pages
      @pageIndex++
      App.fetchList false


  App.getActiveQualityProfile = ->
    value = @activeInFilter.get('value')
    if value? && value.length == 1 then value[0] else null


  App.getInactiveQualityProfile = ->
    value = @inactiveInFilter.get('value')
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
    @codingRules.sorting = sort: 'CREATION_DATE', asc: false


  # Construct status bar
  App.addInitializer ->
    @codingRulesActionsView = new CodingRulesActionsView
      app: @
      collection: @codingRules
    @layout.actionsRegion.show @codingRulesActionsView


  # Construct bulk change views
  App.addInitializer ->
    @codingRulesBulkChangeView = new CodingRulesBulkChangeView app: @
    @codingRulesBulkChangeDropdownView = new CodingRulesBulkChangeDropdownView app: @


  # Construct quality profile activation view
  App.addInitializer ->
    @codingRulesQualityProfileActivationView = new CodingRulesQualityProfileActivationView app: @


  # Define filters
  App.addInitializer ->
    @filters = new BaseFilters.Filters

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.name'
      property: 'name'
      type: StringFilterView

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.language'
      property: 'languages'
      type: ChoiceFilters.ChoiceFilterView
      choices: @languages

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.severity'
      property: 'severities'
      type: ChoiceFilters.ChoiceFilterView
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
      name: t 'coding_rules.filters.tag'
      property: 'tags'
      type: ChoiceFilters.ChoiceFilterView
      choices: @tags

    @activeInFilter = new BaseFilters.Filter
      name: t 'coding_rules.filters.in_quality_profile'
      property: 'in_quality_profile'
      type: QualityProfileFilterView
      multiple: false
    @filters.add @activeInFilter


    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.key'
      property: 'key'
      type: StringFilterView
      enabled: false
      optional: true

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.description'
      property: 'description'
      type: StringFilterView
      enabled: false
      optional: true

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.repository'
      property: 'repositories'
      type: ChoiceFilters.ChoiceFilterView
      enabled: false
      optional: true
      choices: @repositories

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.status'
      property: 'statuses'
      type: ChoiceFilters.ChoiceFilterView
      enabled: false
      optional: true
      choices: @statuses

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.availableSince'
      property: 'availableSince'
      type: DateFilterView
      enabled: false
      optional: true

    @inactiveInFilter = new BaseFilters.Filter
      name: t 'coding_rules.filters.out_of_quality_profile'
      property: 'out_of_quality_profile'
      type: QualityProfileFilterView
      multiple: false
      enabled: false
      optional: true
    @filters.add @inactiveInFilter

    @filters.add new BaseFilters.Filter
      name: t 'coding_rules.filters.inheritance'
      property: 'inheritance'
      type: InheritanceFilterView
      enabled: false
      optional: true
      multiple: false
      qualityProfileFilter: @activeInFilter
      choices:
        'any': t 'coding_rules.filters.inheritance.any'
        'not_inhertited': t 'coding_rules.filters.inheritance.not_inherited'
        'inhertited': t 'coding_rules.filters.inheritance.inherited'
        'overriden': t 'coding_rules.filters.inheritance.overriden'

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
      App.languages = r.languages
      App.repositories = r.repositories
      App.statuses = r.statuses
      App.tags = r.tags
      window.messages = r.messages

      # Remove the initial spinner
      jQuery('#coding-rules-page-loader').remove()

      # Start the application
      App.start()