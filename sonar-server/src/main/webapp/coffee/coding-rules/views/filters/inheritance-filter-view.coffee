define [
  'navigator/filters/choice-filters'
], (
  ChoiceFilters
) ->

  class InheritanceFilterView extends ChoiceFilters.ChoiceFilterView

    initialize: ->
      super
      @qualityProfileFilter = @model.get 'qualityProfileFilter'
      @listenTo @qualityProfileFilter, 'change:value', @onChangeQualityProfile
      @onChangeQualityProfile()


    onChangeQualityProfile: ->
      qualityProfile = @qualityProfileFilter.get 'value'
      parentQualityProfile = @qualityProfileFilter.get 'parentQualityProfile'
      if _.isArray(qualityProfile) && qualityProfile.length == 1 && parentQualityProfile
        @makeActive()
      else
        @makeInactive()


    makeActive: ->
      @model.set inactive: false, title: ''
      @model.trigger 'change:enabled'
      @$el.removeClass('navigator-filter-inactive').prop 'title', ''


    makeInactive: ->
      @model.set inactive: true, title: t 'coding_rules.filters.inheritance.inactive'
      @model.trigger 'change:enabled'
      @choices.each (model) -> model.set 'checked', false
      @detailsView.updateLists()
      @detailsView.updateValue()
      @$el.addClass('navigator-filter-inactive').prop 'title', t 'coding_rules.filters.inheritance.inactive'


    showDetails: ->
      super unless @$el.is '.navigator-filter-inactive'


    restore: (value) ->
      value = value.split(',') if _.isString(value)
      if @choices && value.length > 0
        @model.set value: value, enabled: true
        @onChangeQualityProfile
      else
        @clear()
