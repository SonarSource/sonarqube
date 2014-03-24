define [
  'coding-rules/views/filters/inheritance-filter-view'
], (
  InheritanceFilterView
) ->

  class ActivationFilterView extends InheritanceFilterView
    tooltip: 'coding_rules.filters.activation.help'


    onChangeQualityProfile: ->
      qualityProfile = @qualityProfileFilter.get 'value'
      if _.isArray(qualityProfile) && qualityProfile.length == 1 then @makeActive() else @makeInactive()