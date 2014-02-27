define [
  'backbone',
  'quality-gate/models/metric'
], (
  Backbone,
  Metric
) ->

  class Metrics extends Backbone.Collection
    model: Metric


    url:
      "#{baseUrl}/api/qualitygates/metrics"


    parse: (r) ->
      r.metrics
