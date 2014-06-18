define [], () ->

  $ = jQuery
  API_SCM = "#{baseUrl}/api/sources/scm"


  class SCMMixin

    requestSCM: (key) ->
      $.get API_SCM, key: key, (data) =>
        if data?.scm?
          @state.set 'hasSCM', true
          @source.set scm: data.scm
          @augmentWithSCM data.scm


    augmentWithSCM: (scm) ->
      formattedSource = @source.get 'formattedSource'
      scmLength = scm.length
      if scmLength > 0
        scmIndex = 0
        scmCurrent = scm[scmIndex]
        scmDetails = {}
        formattedSource.forEach (line) ->
          if line.lineNumber == scmCurrent[0]
            scmDetails = author: scmCurrent[1], date: scmCurrent[2]
            if scmIndex < scmLength - 1
              scmIndex++
              scmCurrent = scm[scmIndex]
          line.scm = scmDetails
        @source.set 'formattedSource', formattedSource



    showSCM: (store = false) ->
      @settings.set 'scm', true
      @storeSettings() if store
      unless @state.get 'hasSCM'
        @requestSCM(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideSCM: (store = false) ->
      @settings.set 'scm', false
      @storeSettings() if store
      @sourceView.render()


    filterBySCM: ->
      requests = [@requestSCM(@key)]
      if @settings.get('issues') && !@state.get('hasIssues')
        requests.push @requestIssues @key
      $.when.apply($, requests).done =>
        @filterByUnresolvedIssues()
        @_filterBySCM()


    _filterBySCM: () ->
      formattedSource = @source.get 'formattedSource'
      period = @state.get 'period'
      unless period?
        return @showAllLines()
      else
        periodDate = period.get 'sinceDate'
      @settings.set 'scm', true
      @sourceView.resetShowBlocks()
      scmBlockLine = 1
      predicate = false
      formattedSource.forEach (line) =>
        scmBlockDate = new Date line.scm.date
        if scmBlockDate >= periodDate
          scmBlockLine = line.lineNumber if predicate == false
          predicate = true
        else if predicate == true
          predicate = false
          @sourceView.addShowBlock scmBlockLine, line.lineNumber - 1
      if predicate
        @sourceView.addShowBlock scmBlockLine, _.size @source.get 'source'
      @sourceView.render()