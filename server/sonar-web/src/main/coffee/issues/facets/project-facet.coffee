define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      q = @options.app.state.get 'contextComponentQualifier'
      if q == 'VW' || q == 'SVW'
        "#{baseUrl}/api/components/search"
      else
        "#{baseUrl}/api/resources/search?f=s2&q=TRK&display_uuid=true"


    prepareSearch: ->
      q = @options.app.state.get 'contextComponentQualifier'
      if q == 'VW' || q == 'SVW'
        @prepareSearchForViews()
      else super


    prepareSearchForViews: ->
      componentUuid = this.options.app.state.get 'contextComponentUuid'
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
          data: (term, page) ->
            q: term
            componentUuid: componentUuid
            p: page
            ps: 25
          results: (data) ->
            more: data.p * data.ps < data.total,
            results: data.components.map (c) -> id: c.uuid, text: c.name


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
        values: @sortValues @getValuesWithLabels()
