define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      "#{baseUrl}/api/resources/search?f=s2&q=TRK&display_uuid=true"


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
