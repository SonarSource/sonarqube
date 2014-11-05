define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      "#{baseUrl}/api/users/search?f=s2"


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
        values: @sortValues @getValuesWithLabels()
