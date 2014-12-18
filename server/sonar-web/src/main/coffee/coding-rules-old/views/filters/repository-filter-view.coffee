define [
  'navigator/filters/choice-filters',
  'templates/coding-rules-old'
], (
  ChoiceFilters,
  Templates
) ->

  class RepositoryDetailFilterView extends ChoiceFilters.DetailsChoiceFilterView
    itemTemplate: Templates['coding-rules-repository-detail']


  class RepositoryFilterView extends ChoiceFilters.ChoiceFilterView

    initialize: ->
      super
        detailsView: RepositoryDetailFilterView

      @app = @model.get 'app'

      @allRepositories = @model.get 'choices'
      @updateChoices @allRepositories

      @listenTo @app.languageFilter, 'change:value', @onChangeLanguage
      @onChangeLanguage()


    onChangeLanguage: ->
      languages = @app.languageFilter.get 'value'
      if _.isArray(languages) && languages.length > 0
        @filterLanguages(languages)
      else
        @updateChoices(@allRepositories)

    filterLanguages: (languages) ->
      languageRepositories = _.filter( @allRepositories, (repo) -> languages.indexOf(repo.language) >= 0 )
      @updateChoices(languageRepositories)


    updateChoices: (collection) ->
      languages = @app.languages
      currentValue = @model.get('value')
      @choices = new Backbone.Collection( _.map collection, (item, index) ->
          new Backbone.Model
            id: item.key
            text: item.name
            checked: false
            index: index
            language: languages[item.language]
        comparator: (item) ->
          [item.get('text'), item.get('language')]
      )
      if currentValue
        @restore(currentValue)
      @render()
