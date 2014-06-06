define [], () ->

  $ = jQuery
  API_COVERAGE = "#{baseUrl}/api/coverage/show"
  LINES_AROUND_COVERED_LINE = 1


  class CoverageMixin

    requestCoverage: (key, type = 'UT') ->
      $.get API_COVERAGE, key: key, type: type, (data) =>
        @state.set 'hasCoverage', true
        @source.set coverage: data.coverage


    showCoverage: (store = false) ->
      @settings.set 'coverage', true
      @storeSettings() if store
      unless @state.get 'hasCoverage'
        @requestCoverage(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideCoverage: (store = false) ->
      @settings.set 'coverage', false
      @storeSettings() if store
      @sourceView.render()


    filterByCoverage: (predicate) ->
      @requestCoverage(@key).done => @_filterByCoverage(predicate)


    filterByCoverageIT: (predicate) ->
      @requestCoverage(@key, 'IT').done => @_filterByCoverage(predicate)


    _filterByCoverage: (predicate) ->
      coverage = @source.get 'coverage'
      @settings.set 'coverage', true
      @sourceView.resetShowBlocks()
      coverage.forEach (c) =>
        if predicate c
          line = c[0]
          @sourceView.addShowBlock line - LINES_AROUND_COVERED_LINE, line + LINES_AROUND_COVERED_LINE
      @sourceView.render()


    # Unit Tests
    filterByLinesToCover: -> @filterByCoverage (c) -> c[1]?
    filterByUncoveredLines: -> @filterByCoverage (c) -> c[1]? && !c[1]
    filterByBranchesToCover: -> @filterByCoverage (c) -> c[3]?
    filterByUncoveredBranches: -> @filterByCoverage (c) -> c[3]? && c[4]? && (c[3] > c[4])

    # Integration Tests
    filterByLinesToCoverIT: -> @filterByCoverageIT (c) -> c[1]?
    filterByUncoveredLinesIT: -> @filterByCoverageIT (c) -> c[1]? && !c[1]
    filterByBranchesToCoverIT: -> @filterByCoverageIT (c) -> c[3]?
    filterByUncoveredBranchesIT: -> @filterByCoverageIT (c) -> c[3]? && c[4]? && (c[3] > c[4])