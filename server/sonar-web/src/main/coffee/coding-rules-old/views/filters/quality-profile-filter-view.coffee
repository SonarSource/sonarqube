define [
  'navigator/filters/choice-filters',
  'templates/coding-rules-old'
], (
  ChoiceFilters,
  Templates
) ->

  class QualityProfileDetailFilterView extends ChoiceFilters.DetailsChoiceFilterView
    itemTemplate: Templates['coding-rules-profile-filter-detail']


  class QualityProfileFilterView extends ChoiceFilters.ChoiceFilterView

    initialize: ->
      super
        detailsView: QualityProfileDetailFilterView

      @app = @model.get 'app'

      @allProfiles = @model.get 'choices'
      @updateChoices @allProfiles

      @listenTo @app.languageFilter, 'change:value', @onChangeLanguage
      @onChangeLanguage()


    onChangeLanguage: ->
      languages = @app.languageFilter.get 'value'
      if _.isArray(languages) && languages.length > 0
        @filterLanguages(languages)
      else
        @updateChoices(@allProfiles)

    filterLanguages: (languages) ->
      languageProfiles = _.filter( @allProfiles, (prof) -> languages.indexOf(prof.lang) >= 0 )
      @updateChoices(languageProfiles)


    updateChoices: (collection) ->
      languages = @app.languages
      currentValue = @model.get('value')
      @choices = new Backbone.Collection( _.map collection, (item, index) ->
          new Backbone.Model
            id: item.key
            text: item.name
            checked: false
            index: index
            language: languages[item.lang]
        comparator: 'index'
      )
      if currentValue
        @restore(currentValue)
      @render()

    render: ->
      super
      if @model.get 'value'
        @$el.addClass('navigator-filter-context')
      else
        @$el.removeClass('navigator-filter-context')
