define [
  'quality-gate/models/condition'
], (
  Condition
) ->

  class Conditions extends Backbone.Collection
    model: Condition
    comparator: 'metric'
