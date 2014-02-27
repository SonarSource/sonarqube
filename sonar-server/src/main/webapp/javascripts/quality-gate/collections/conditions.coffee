define [
  'backbone',
  'quality-gate/models/condition'
], (
  Backbone,
  Condition
) ->

  class Conditions extends Backbone.Collection
    model: Condition
