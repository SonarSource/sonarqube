define [], () ->

  $ = jQuery
  API_DUPLICATIONS = "#{baseUrl}/api/duplications/show"
  LINES_AROUND_DUPLICATION = 1


  class DuplicationsMixin

    requestDuplications: (key) ->
      $.get API_DUPLICATIONS, key: key, (data) =>
        @state.set 'hasDuplications', true
        @source.set duplications: data.duplications
        @source.set duplicationFiles: data.files


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
      unless @state.get 'hasDuplications'
        @requestDuplications(@key).done => @_filterByDuplications()
      else
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