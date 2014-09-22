define [
  'navigator/filters/choice-filters'
], (
  ChoiceFilters
) ->

  class ProfileDependentFilterView extends ChoiceFilters.ChoiceFilterView
    tooltip: 'coding_rules.filters.activation.help'

    initialize: ->
      super
      @qualityProfileFilter = @model.get 'qualityProfileFilter'
      @listenTo @qualityProfileFilter, 'change:value', @onChangeQualityProfile
      @onChangeQualityProfile()


    onChangeQualityProfile: ->
      qualityProfileKey = @qualityProfileFilter.get 'value'
      if _.isArray(qualityProfileKey) && qualityProfileKey.length == 1
        @makeActive()
      else
        @makeInactive()


    makeActive: ->
      @model.set inactive: false, title: ''
      @model.trigger 'change:enabled'
      @$el.removeClass('navigator-filter-inactive').prop 'title', ''
      @options.filterBarView.moreCriteriaFilter.view.detailsView.enableByProperty(@detailsView.model.get 'property')
      @hideDetails()


    makeInactive: ->
      @model.set inactive: true, title: t @tooltip
      @model.trigger 'change:enabled'
      @choices.each (model) -> model.set 'checked', false
      @detailsView.updateLists()
      @detailsView.updateValue()
      @$el.addClass('navigator-filter-inactive').prop 'title', t @tooltip


    showDetails: ->
      super unless @$el.is '.navigator-filter-inactive'


    restore: (value) ->
      value = value.split(',') if _.isString(value)
      if @choices && value.length > 0
        @model.set value: value, enabled: true
        @choices.each (item) ->
          item.set 'checked', false
        _.each value, (v) =>
          cModel = @choices.findWhere id: v
          cModel.set 'checked', true
        @onChangeQualityProfile()
      else
        @clear()
