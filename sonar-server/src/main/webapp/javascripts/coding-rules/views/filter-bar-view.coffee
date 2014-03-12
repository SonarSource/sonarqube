define [
  'navigator/filters/filter-bar',
  'navigator/filters/base-filters',
  'navigator/filters/favorite-filters',
  'navigator/filters/more-criteria-filters'
], (
  FilterBarView,
  BaseFilters,
  FavoriteFiltersModule,
  MoreCriteriaFilters
) ->

  class CodingRulesFilterBarView extends FilterBarView

    collectionEvents:
      'change:enabled': 'changeEnabled'


    events:
      'click .navigator-filter-submit': 'search'


    getQuery: ->
      query = {}
      @collection.each (filter) ->
        _.extend query, filter.view.formatValue()
      query


    onAfterItemAdded: (itemView) ->
      if itemView.model.get('type') == FavoriteFiltersModule.FavoriteFilterView
        jQuery('.navigator-header').addClass 'navigator-header-favorite'


    addMoreCriteriaFilter: ->
      disabledFilters = this.collection.where enabled: false
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
          filter.get('type') == MoreCriteriaFilters.MoreCriteriaFilterView

        if disabledFilters.length == 0
          @moreCriteriaFilter.set { enabled: false }, { silent: true }
        else
          @moreCriteriaFilter.set { enabled: true }, { silent: true }

        @moreCriteriaFilter.set { filters: disabledFilters }, { silent: true }
        @moreCriteriaFilter.trigger 'change:filters'


    search: ->
      @options.app.state.set
        query: this.options.app.getQuery(),
        search: true
      @options.app.fetchFirstPage()


    fetchNextPage: ->
      @options.app.fetchNextPage()
