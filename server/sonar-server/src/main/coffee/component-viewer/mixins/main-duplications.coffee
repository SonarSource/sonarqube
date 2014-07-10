define [], () ->

  $ = jQuery
  API_DUPLICATIONS = "#{baseUrl}/api/duplications/show"
  LINES_AROUND_DUPLICATION = 1


  class DuplicationsMixin

    requestDuplications: (key) ->
      $.get API_DUPLICATIONS, key: key, (data) =>
        return unless data?.duplications?
        @state.set 'hasDuplications', true
        @source.set duplications: data.duplications
        @source.set duplicationFiles: data.files
        @skipRemovedFiles()
        @augmentWithDuplications data.duplications


    skipRemovedFiles: ->
      duplications = @source.get 'duplications'
      deletedFiles = false
      duplications = _.map duplications, (d) ->
        blocks = _.filter d.blocks, (b) -> b._ref
        deletedFiles = true if blocks.length != d.blocks.length
        blocks: blocks
      @source.set 'duplications', duplications
      @state.set 'duplicationsInDeletedFiles', deletedFiles


    augmentWithDuplications: (duplications) ->
      formattedSource = @source.get 'formattedSource'
      return unless formattedSource
      formattedSource.forEach (line) ->
        lineDuplications = []
        duplications.forEach (d, i) ->
          duplicated = false
          d.blocks.forEach (b) ->
            if b._ref == '1'
              lineFrom = b.from
              lineTo = b.from + b.size
              duplicated = true if line.lineNumber >= lineFrom && line.lineNumber <= lineTo
          lineDuplications.push if duplicated then i + 1 else false
        line.duplications = lineDuplications
      @source.set 'formattedSource', formattedSource


    showDuplications: (store = false) ->
      @settings.set 'duplications', true
      @storeSettings() if store
      unless @state.get 'hasDuplications'
        @requestDuplications(@key).done => @sourceView.render()
      else
        @sourceView.render()


    hideDuplications: (store = false) ->
      @settings.set 'duplications', false
      @storeSettings() if store
      @sourceView.render()


    # Duplications
    filterByDuplications: ->
      requests = [@requestDuplications(@key)]
      if @settings.get('issues') && !@state.get('hasIssues')
        requests.push @requestIssues @key
      $.when.apply($, requests).done =>
        @_filterByDuplications()


    _filterByDuplications: ->
      duplications = @source.get 'duplications'
      @settings.set 'duplications', true
      @sourceView.resetShowBlocks()
      duplications.forEach (d) =>
        d.blocks.forEach (b) =>
          if b._ref == '1'
            lineFrom = b.from
            lineTo = b.from + b.size
            @sourceView.addShowBlock lineFrom - LINES_AROUND_DUPLICATION, lineTo + LINES_AROUND_DUPLICATION
      @sourceView.render()
