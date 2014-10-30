define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

    getValuesWithLabels: ->
      values = @model.getValues()
      source = @options.app.facets.components
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          item = _.findWhere source, uuid: key
          label = item.longName if item?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
