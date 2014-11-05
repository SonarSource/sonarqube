define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

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
