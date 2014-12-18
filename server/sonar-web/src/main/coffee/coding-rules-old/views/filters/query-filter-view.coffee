define [
  'backbone',
  'backbone.marionette',
  'navigator/filters/base-filters',
  'navigator/filters/string-filters',
  'navigator/filters/choice-filters',
  'templates/coding-rules-old',
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
      'keypress input': 'checkSubmit'
      'change input': 'change'
      'click': 'focus'
      'blur': 'blur'


    change: (e) ->
      @model.set 'value', $j(e.target).val()
      @options.app.codingRules.sorting = sort: '', asc: ''


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


    checkSubmit: (e) ->
      if (e.which == 13)
        e.preventDefault()
        @change(e)
        @blur()
        @options.app.filterBarView.$('.navigator-filter-submit').focus()
        @options.app.filterBarView.$('.navigator-filter-submit').click()


    renderInput: ->
      # Done in template


    toggleDetails: ->
      # NOP


    isDefaultValue: ->
      true


    renderBase: ->
      super
      @$el.prop('title', '');
