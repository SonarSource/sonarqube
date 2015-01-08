define ->

  class extends Backbone.Model
    idAttribute: 'property'


    defaults:
      enabled: false


    getValues: ->
      @get('values') || []


    toggle: ->
      enabled = @get 'enabled'
      @set enabled: !enabled
