define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->

  class extends BaseFacet

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
