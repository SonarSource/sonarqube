define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model
    idAttribute: 'property'


    defaults:
      enabled: false


    getValues: ->
      console.log @toJSON()
      @get('values') || []


    toggle: ->
      enabled = @get 'enabled'
      @set enabled: !enabled
