define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model

    defaults:
      hasSourceBefore: false
      hasSourceAfter: false

