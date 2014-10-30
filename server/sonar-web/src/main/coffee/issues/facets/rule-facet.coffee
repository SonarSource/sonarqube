define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-rule-facet']


    events: ->
      _.extend super,
        'change .js-issues-custom-value': 'addCustomValue'


    onRender: ->
      super
      @$('.js-issues-custom-value').select2
        placeholder: 'Add rule'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: "#{baseUrl}/api/rules/search?f=name"
          data: (term, page) -> { q: term, p: page }
          results: (data) ->
            results = data.rules.map (rule) ->
              id: rule.key, text: rule.name
            { more: (data.p * data.ps < data.total), results: results }


    getValuesWithLabels: ->
      values = @model.getValues()
      rules = @options.app.facets.rules
      values.forEach (v) =>
        key = v.val
        label = ''
        if key
          rule = _.findWhere rules, key: key
          label = rule.name if rule?
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
