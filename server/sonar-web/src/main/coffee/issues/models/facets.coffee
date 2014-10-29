define [
  'backbone'
  'issues/models/facet'
], (
  Backbone
  Facet
) ->

  class extends Backbone.Collection
    model: Facet
