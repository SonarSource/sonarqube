define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-custom-values-facet']


    events: ->
      _.extend super,
        'change .js-custom-value': 'addCustomValue'


    getUrl: ->


    onRender: ->
      super
      @prepareSearch()


    prepareSearch: ->
      @$('.js-issues-custom-value').select2
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
          data: (term, page) -> { s: term, p: page }
          results: (data) -> { more: data.more, results: data.results }


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      @options.app.state.updateFilter obj
