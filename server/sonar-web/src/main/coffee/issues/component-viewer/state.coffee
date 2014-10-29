define [
  'backbone'
], (
  Backbone
) ->

  class extends Backbone.Model

    defaults:
      hasSourceBefore: true
      hasSourceAfter: true

