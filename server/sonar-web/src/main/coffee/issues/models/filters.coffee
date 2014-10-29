define [
  'backbone'
  'issues/models/filter'
], (
  Backbone
  Filter
) ->

  class extends Backbone.Collection
    model: Filter
