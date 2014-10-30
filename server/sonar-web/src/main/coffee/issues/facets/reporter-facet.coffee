define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

    getValuesWithLabels: ->
      values = @model.getValues()
      source = @options.app.facets.users
      values.forEach (v) =>
        key = v.val
        label = null
        if key
          item = _.findWhere source, login: key
          label = item.name if item?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
