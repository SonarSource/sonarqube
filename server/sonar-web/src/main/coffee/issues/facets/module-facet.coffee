define [
  'issues/facets/custom-values-facet'
], (
  CustomValuesFacet
) ->


  class extends CustomValuesFacet

    getUrl: ->
      "#{baseUrl}/api/resources/search?f=s2&q=BRC&display_uuid=true"


    getValues: ->
      componentRoots = @options.app.state.get('query').componentRootUuids
      componentRoots = '' unless typeof componentRoots == typeof ''
      if componentRoots.length > 0
        componentRoots = componentRoots.split ','
        componentRoots.map (c) ->
          { val: c, count: null }
      else []


    getValuesWithLabels: ->
      values = @getValues()
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
        values: @getValuesWithLabels()
