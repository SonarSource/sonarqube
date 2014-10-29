define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

    getValuesWithLabels: ->
      values = @model.getValues()
      rules = @options.app.facets.rules
      values.forEach (v) =>
        key = v.val
        label = ''
        if key
          rule = _.findWhere rules, key: key
          label = rule.name if rule?
        v.label = label
      values


    serializeData: ->
      _.extend super,
        values: @getValuesWithLabels()
