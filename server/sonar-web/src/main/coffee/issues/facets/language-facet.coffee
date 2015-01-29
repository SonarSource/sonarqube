define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      "#{baseUrl}/api/languages/list"


    prepareSearch: ->
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
          url: @getUrl()
          data: (term) -> { q: term, ps: 0 }
          results: (data) ->
            more: false
            results: data.languages.map (lang) -> { id: lang.key, text: lang.name }


    getValuesWithLabels: ->
      values = @model.getValues()
      source = @options.app.facets.languages
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          item = _.findWhere source, key: key
          label = item.name if item?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
