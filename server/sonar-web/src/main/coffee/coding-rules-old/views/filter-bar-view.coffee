define [
  'navigator/filters/filter-bar',
  'navigator/filters/base-filters',
  'navigator/filters/favorite-filters',
  'navigator/filters/more-criteria-filters',
  'navigator/filters/read-only-filters',
  'templates/coding-rules-old'
], (
  FilterBarView,
  BaseFilters,
  FavoriteFiltersModule,
  MoreCriteriaFilters,
  ReadOnlyFilterView,
  Templates
) ->

  class CodingRulesFilterBarView extends FilterBarView
    template: Templates['coding-rules-filter-bar']

    collectionEvents:
      'change:enabled': 'changeEnabled'


    events:
      'click .navigator-filter-submit': 'search'


    onRender: ->
      @selectFirst()


    getQuery: ->
      query = {}
      @collection.each (filter) ->
        _.extend query, filter.view.formatValue()
      query


    onAfterItemAdded: (itemView) ->
      if itemView.model.get('type') == FavoriteFiltersModule.FavoriteFilterView
        jQuery('.navigator-header').addClass 'navigator-header-favorite'


    addMoreCriteriaFilter: ->
      readOnlyFilters = @collection.where(type: ReadOnlyFilterView)
      disabledFilters = _.difference(@collection.where(enabled: false), readOnlyFilters)
      if disabledFilters.length > 0
        @moreCriteriaFilter = new BaseFilters.Filter
          type: MoreCriteriaFilters.MoreCriteriaFilterView,
          enabled: true,
          optional: false,
          filters: disabledFilters
        @collection.add @moreCriteriaFilter


    changeEnabled: ->
      if @moreCriteriaFilter?
        disabledFilters = _.reject @collection.where(enabled: false), (filter) ->
          filter.get('type') in [MoreCriteriaFilters.MoreCriteriaFilterView, ReadOnlyFilterView]

        if disabledFilters.length == 0
          @moreCriteriaFilter.set { enabled: false }, { silent: true }
        else
          @moreCriteriaFilter.set { enabled: true }, { silent: true }

        @moreCriteriaFilter.set { filters: disabledFilters }, { silent: true }
        @moreCriteriaFilter.trigger 'change:filters'


    search: ->
      @$('.navigator-filter-submit').blur()
      @options.app.state.set
        query: this.options.app.getQuery(),
        search: true
      @options.app.fetchFirstPage()


    fetchNextPage: ->
      @options.app.fetchNextPage()


    restoreFromWsQuery: (query) ->
      params = _.map(query, (value, key) ->
        'key': key
        'value': value
      )
      @restoreFromQuery params


    toggle: (property, value) ->
      filter = @collection.findWhere(property: property)
      unless filter.view.isActive()
        @moreCriteriaFilter.view.detailsView.enableByProperty(property)
      choice = filter.view.choices.get(value)
      choice.set 'checked', !choice.get('checked')
      filter.view.detailsView.updateValue()
      filter.view.detailsView.updateLists()
