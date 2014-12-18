define [
  'navigator/filters/choice-filters',
  'templates/coding-rules-old'
], (
  ChoiceFilters,
  Templates
) ->

  class LanguageFilterView extends ChoiceFilters.ChoiceFilterView

    modelEvents:
      'change:value': 'onChangeValue'
      'change:enabled': 'focus',


    initialize: ->
      super
      @choices.comparator = 'text'
      @choices.sort()
      @app = @model.get 'app'
      @listenTo @app.qualityProfileFilter, 'change:value', @onChangeProfile
      @selectedFromProfile = false

    onChangeProfile: ->
      profiles = @app.qualityProfileFilter.get 'value'
      if _.isArray(profiles) && profiles.length > 0
        profile = _.findWhere @app.qualityProfiles, key: profiles[0]
        @options.filterBarView.moreCriteriaFilter.view.detailsView.enableByProperty(@detailsView.model.get 'property')
        @choices.each (item) -> item.set 'checked', item.id == profile.lang
        @refreshValues()
        @selectedFromProfile = true
      else if @selectedFromProfile
        @choices.each (item) -> item.set 'checked', false
        @refreshValues()

    onChangeValue: ->
      @selectedFromProfile = false
      @renderBase()


    refreshValues: ->
      @detailsView.updateValue()
      @detailsView.updateLists()
      @render()
      @hideDetails()
