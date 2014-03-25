define [
  'navigator/filters/choice-filters'
  'coding-rules/views/filters/inheritance-filter-view'
], (
  ChoiceFilters
  InheritanceFilterView
) ->

  class DetailsActivationFilterView extends ChoiceFilters.DetailsChoiceFilterView

    onCheck: (e) ->
      id = jQuery(e.target).val()
      selected = @options.filterView.choices.findWhere checked: true
      unless id == selected
        @options.filterView.choices.each (item) -> item.set 'checked', item.id == id
      else
        e.preventDefault()
      @updateValue()
      @updateLists()



  class ActivationFilterView extends InheritanceFilterView
    tooltip: 'coding_rules.filters.activation.help'


    initialize: ->
      super detailsView: DetailsActivationFilterView


    onChangeQualityProfile: ->
      qualityProfile = @qualityProfileFilter.get 'value'
      if _.isArray(qualityProfile) && qualityProfile.length == 1 then @makeActive() else @makeInactive()


    makeActive: ->
      @choices.each (item) -> item.set 'checked', item.id == 'active'
      @detailsView.updateValue()
      @detailsView.updateLists()
      @render()
      super