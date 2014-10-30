define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-reporter-facet']


    events: ->
      _.extend super,
        'change .js-issues-custom-value': 'addCustomValue'


    onRender: ->
      super
      @$('.js-issues-custom-value').select2
        placeholder: 'Add reporter'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: "#{baseUrl}/api/users/search?f=s2"
          data: (term, page) -> { s: term, p: page }
          results: (data) -> { more: data.more, results: data.results }


    getValuesWithLabels: ->
      values = @model.getValues()
      source = @options.app.facets.users
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          item = _.findWhere source, login: key
          label = item.name if item?
        v.label = label
      values


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-issues-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      @options.app.state.updateFilter obj


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
