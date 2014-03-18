define [
  'backbone',
  'quality-gate/models/quality-gate'
], (
  Backbone,
  QualityGate
) ->

  class QualityGates extends Backbone.Collection
    model: QualityGate


    url: ->
      "#{baseUrl}/api/qualitygates/list"


    # {
    #   "qualitygates": [
    #     { "id": 42, "name": "QG 1" },
    #     { "id": 43, "name": "QG 2" },
    #     { "id": 44, "name": "QG 3" }
    #   ],
    #   "default": 42
    # }
    parse: (r) ->
      r.qualitygates.map (gate) ->
        _.extend gate, default: gate.id == r.default
