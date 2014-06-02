define [
  'navigator/filters/choice-filters',
  'templates/coding-rules'
], (
  ChoiceFilters,
  Templates
) ->

  class LanguageFilterView extends ChoiceFilters.ChoiceFilterView

    initialize: ->
      super
      @app = @model.get 'app'
      @listenTo @app.qualityProfileFilter, 'change:value', @onChangeProfile

    onChangeProfile: ->
      profiles = @app.qualityProfileFilter.get 'value'
      if _.isArray(profiles) && profiles.length > 0
        profile = _.findWhere @app.qualityProfiles, key: profiles[0]
        @restore profile.lang
        # force alignment of details list
        @app.qualityProfileFilter.view.showDetails()
