define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      "#{baseUrl}/api/resources/search?f=s2&q=BRC&display_uuid=true"


    getValuesWithLabels: ->
      values = @model.getValues()
      components = @options.app.facets.components
      values.forEach (v) =>
        uuid = v.val
        label = uuid
        if uuid
          component = _.findWhere components, uuid: uuid
          label = component.longName if component?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @sortValues @getValuesWithLabels()
