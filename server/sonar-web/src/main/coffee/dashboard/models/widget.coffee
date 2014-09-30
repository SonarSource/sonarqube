define ['backbone'], (Backbone) ->


  class extends Backbone.Model

    defaults: ->
      col: 1
      row: 0
      props: []
      configured: false


    mergeProperties: (properties) ->
      props = @get 'properties'
      props = properties.map (prop) ->
        data = _.findWhere props, key: prop.key
        _.extend prop, data
      @set 'properties', props
