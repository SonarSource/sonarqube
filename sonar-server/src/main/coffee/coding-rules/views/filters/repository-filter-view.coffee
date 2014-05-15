define [
  'navigator/filters/choice-filters',
  'templates/coding-rules'
], (
  ChoiceFilters,
  Templates
) ->

  class RepositoryFilterView extends ChoiceFilters.ChoiceFilterView
    itemTemplate: Templates['coding-rules-repository-detail']

    initialize: ->
      super
      @allRepositories = @model.get 'choices'
      @updateChoices @allRepositories

      @languageFilter = @model.get 'languageFilter'
      @listenTo @languageFilter, 'change:value', @onChangeLanguage
      @onChangeLanguage()


    onChangeLanguage: ->
      languages = @languageFilter.get 'value'
      if _.isArray(languages) && languages.length > 0
        @filterLanguages(languages)
      else
        @updateChoices(@allRepositories)

    filterLanguages: (languages) ->
      languageRepositories = _.filter( @allRepositories, (repo) -> languages.indexOf(repo.language) >= 0 )
      @updateChoices(languageRepositories)


    updateChoices: (collection) ->
      currentValue = @model.get('value')
      @choices = new Backbone.Collection( _.map collection, (item, index) ->
          new Backbone.Model
            id: item.key
            text: item.name
            checked: false
            index: index
        comparator: 'index'
      )
      if currentValue
        @restore(currentValue)
      @render()
