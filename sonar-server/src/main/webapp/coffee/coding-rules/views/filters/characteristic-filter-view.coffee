define [
  'navigator/filters/choice-filters'
], (
  ChoiceFilters
) ->

  class CharacteriticFilterView extends ChoiceFilters.ChoiceFilterView

    initialize: ->
      super
      @choices.comparator = 'text'
      @choices.sort()
