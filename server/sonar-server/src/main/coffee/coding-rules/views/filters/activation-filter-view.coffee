define [
  'coding-rules/views/filters/profile-dependent-filter-view'
], (
  ProfileDependentFilterView
) ->

  class ActivationFilterView extends ProfileDependentFilterView
    tooltip: 'coding_rules.filters.activation.help'


    makeActive: ->
      super
      unless @model.get 'value'
        @choices.each (model) -> model.set 'checked', model.id == 'true'
        @model.set 'value', ['true']



    showDetails: ->
      super unless @$el.is '.navigator-filter-inactive'


    restore: (value) ->
      value = value.split(',') if _.isString(value)
      if @choices && value.length > 0
        @choices.each (model) -> model.set 'checked', value.indexOf(model.id) >= 0
        @model.set value: value, enabled: true
        @onChangeQualityProfile()
      else
        @clear()
