define [
  'backbone'
  'analysis-reports/models/report'
], (
  Backbone
  Report
) ->


  class extends Backbone.Collection
    model: Report


    url: ->
      "#{baseUrl}/api/analysis_reports/search"


    parse: (r) ->
      @paging = r.paging
      r.reports
