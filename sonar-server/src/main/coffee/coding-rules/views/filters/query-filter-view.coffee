define [
  'backbone',
  'backbone.marionette',
  'navigator/filters/base-filters',
  'navigator/filters/string-filters',
  'navigator/filters/choice-filters',
  'templates/coding-rules',
  'common/handlebars-extensions'
], (
  Backbone,
  Marionette,
  BaseFilters,
  StringFilterView,
  ChoiceFilters,
  Templates
) ->

  class QueryFilterView extends StringFilterView
    template: Templates['coding-rules-query-filter']
    className: 'navigator-filter navigator-filter-query'

    events:
      'change input': 'change'
      'click': 'focus'
      'blur': 'blur'


    change: (e) ->
      @model.set 'value', $j(e.target).val()
      @options.app.codingRules.sorting = sort: '', asc: ''
      @options.app.fetchFirstPage()


    clear: ->
      super
      @focus()


    focus: ->
      @$(':input').focus();


    blur: ->
      @$(':input').blur();


    serializeData: ->
      return _.extend({}, @model.toJSON(),
        value: this.model.get('value') || ''
      )


    initialize: ->
      super detailsView: null
      @model.set('size', 25) unless @model.get 'size'


    renderInput: ->
      # Done in template


    toggleDetails: ->
      # NOP


    isDefaultValue: ->
      true
