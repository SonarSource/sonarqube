define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    prepareSearch: ->
      url = "#{baseUrl}/api/rules/search?f=name,langName"
      languages = @options.app.state.get('query').languages
      if languages?
        url += "&languages=#{languages}"
      @$('.js-custom-value').select2
        placeholder: 'Search...'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: url
          data: (term, page) -> { q: term, p: page }
          results: (data) ->
            results = data.rules.map (rule) ->
              id: rule.key, text: "(#{rule.langName}) #{rule.name}"
            { more: (data.p * data.ps < data.total), results: results }


    getValuesWithLabels: ->
      values = @model.getValues()
      rules = @options.app.facets.rules
      values.forEach (v) =>
        key = v.val
        label = ''
        extra = ''
        if key
          rule = _.findWhere rules, key: key
          label = rule.name if rule?
          extra = rule.langName if rule?
        v.label = label
        v.extra = extra
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
