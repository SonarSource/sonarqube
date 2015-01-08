define [
  'analysis-reports/models/report'
], (
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
      @fetch { url: "#{baseUrl}/api/computation/queue" }, { reset: true }


    fetchHistory: (options = {  }) ->
      _.extend options,
        url: "#{baseUrl}/api/computation/history"
      options.data = options.data || {}
      options.data.ps = 50
      @fetch options, { reset: true }
