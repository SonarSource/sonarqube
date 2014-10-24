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
      @paging =
        page: r.p
        pageSize: r.ps
        total: r.total
        maxResultsReached: r.p * r.ps >= r.total
      r.reports


    fetchActive: ->
      @fetch { url: "#{baseUrl}/api/analysis_reports/active" }, { reset: true }


    fetchHistory: (options = {  }) ->
      _.extend options,
        url: "#{baseUrl}/api/analysis_reports/history"
      @fetch options, { reset: true }
