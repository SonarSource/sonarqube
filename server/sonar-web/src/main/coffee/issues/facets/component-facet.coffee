define [
  'issues/facets/base-facet'
  'templates/issues'
], (
  BaseFacet
  Templates
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-component-facet']


    onRender: ->
      super
      maxValueWidth = _.max @$('.facet-stat').map(-> $(@).outerWidth()).get()
      @$('.facet-name').css 'padding-right', maxValueWidth


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
        values: @sortValues @getValuesWithLabels()
