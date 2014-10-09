define [
  'backbone'
  'analysis-reports/models/report'
], (
  Backbone
  Report
) ->


  class extends Backbone.Collection
    model: Report


    parse: (r) ->
      @paging = r.paging
      r.reports


    fetchActive: ->
      @fetch { url: "#{baseUrl}/api/analysis_reports/active" }, { reset: true }


    fetchHistory: ->
      @fetch { url: "#{baseUrl}/api/analysis_reports/history" }, { reset: true }
