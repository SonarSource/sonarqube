define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->


  class extends BaseFacet
    template: Templates['issues-project-facet']


    events: ->
      _.extend super,
        'change .js-issues-custom-value': 'addCustomValue'


    onRender: ->
      super
      @$('.js-issues-custom-value').select2
        placeholder: 'Add project'
        minimumInputLength: 2
        allowClear: false
        formatNoMatches: -> t 'select2.noMatches'
        formatSearching: -> t 'select2.searching'
        formatInputTooShort: -> tp 'select2.tooShort', 2
        width: '100%'
        ajax:
          quietMillis: 300
          url: "#{baseUrl}/api/resources/search?f=s2&q=TRK&display_uuid=true"
          data: (term, page) -> { s: term, p: page }
          results: (data) -> { more: data.more, results: data.results }


    addCustomValue: ->
      property = @model.get 'property'
      customValue = @$('.js-issues-custom-value').select2 'val'
      value = @getValue()
      value += ',' if value.length > 0
      value += customValue
      obj = {}
      obj[property] = value
      @options.app.state.updateFilter obj


    getValuesWithLabels: ->
      values = @model.getValues()
      projects = @options.app.facets.projects
      values.forEach (v) =>
        uuid = v.val
        label = ''
        if uuid
          project = _.findWhere projects, uuid: uuid
          label = project.longName if project?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
