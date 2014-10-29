define [
  'issues/facets/base-facet'
], (
  BaseFacet
) ->


  class extends BaseFacet

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
        values: @getValuesWithLabels()
