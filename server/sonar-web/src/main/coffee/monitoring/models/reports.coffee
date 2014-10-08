define [
  'backbone'
  'monitoring/models/report'
], (
  Backbone
  Report
) ->


  class extends Backbone.Collection
    model: Report


    url: ->
      "#{baseUrl}/api/reports/search"


    parse: (r) ->
      @paging = r.paging
      r.reports
