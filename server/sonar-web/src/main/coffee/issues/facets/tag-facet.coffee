define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    prepareSearch: ->
      url = "#{baseUrl}/api/issues/tags?ps=10"
      tags = @options.app.state.get('query').tags
      if tags?
        url += "&tags=#{tags}"
      @$('.js-custom-value').select2
        placeholder: 'Search...'
        minimumInputLength: 0
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        width: '100%'
        ajax:
          quietMillis: 300
          url: url
          data: (term, page) -> { q: term, ps: 10 }
          results: (data) ->
            results = data.tags.map (tag) ->
              id: tag, text: tag
            { more: false, results: results }


    getValuesWithLabels: ->
      values = @model.getValues()
      tags = @options.app.facets.tags
      values.forEach (v) =>
        v.label = v.val
        v.extra = ''
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
