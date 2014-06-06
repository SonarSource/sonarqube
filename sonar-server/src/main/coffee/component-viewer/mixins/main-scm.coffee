define [], () ->

  $ = jQuery
  API_SCM = "#{baseUrl}/api/sources/scm"


  class SCMMixin

    requestSCM: (key) ->
      $.get API_SCM, key: key, (data) =>
        @state.set 'hasSCM', true
        @source.set scm: data.scm


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