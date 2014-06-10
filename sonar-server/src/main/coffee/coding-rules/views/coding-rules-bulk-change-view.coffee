define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesBulkChangeView extends Marionette.ItemView
    className: 'modal'
    template: Templates['coding-rules-bulk-change']


    events:
      'submit form': 'onSubmit'
      'click #coding-rules-cancel-bulk-change': 'hide'
      'change select': 'enableAction'


    onRender: ->
      @$el.dialog
        dialogClass: 'no-close',
        width: '600px',
        draggable: false,
        autoOpen: false,
        modal: true,
        minHeight: 50,
        resizable: false,
        title: null

      @$('#coding-rules-bulk-change-profile').select2
        width: '250px'
        minimumResultsForSearch: 1

      format = (state) ->
        return state.text unless state.id
        "<i class='icon-severity-#{state.id.toLowerCase()}'></i> #{state.text}"
      @$('#coding-rules-bulk-change-severity').select2
        width: '250px'
        minimumResultsForSearch: 999
        formatResult: format
        formatSelection: format
        escapeMarkup: (m) -> m


    show: (action) ->
      @action = action
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    prepareQuery: ->
      query = @options.app.getQuery()

      if @action == 'activate' || @action == 'deactivate'
        _.extend query,
          wsAction: @action
          profile_key: @$('#coding-rules-bulk-change-profile').val()

      if @action == 'change-severity'
        _.extend query,
          wsAction: 'activate'
          profile_key: @options.app.getQualityProfile()
          activation_severity: @$('#coding-rules-bulk-change-severity').val()

      query


    bulkChange: (query) ->
      wsAction = query.wsAction
      query = _.omit(query, 'wsAction')
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualityprofiles/#{wsAction}_rules"
        data: query
      .done =>
        @options.app.fetchFirstPage(true)


    onSubmit: (e) ->
      e.preventDefault()
      @bulkChange(@prepareQuery()).done => @hide()


    getAvailableQualityProfiles: ->
      languages = @options.app.languageFilter.get('value')
      singleLanguage = _.isArray(languages) && languages.length == 1

      if singleLanguage
        @options.app.getQualityProfilesForLanguage(languages[0])
      else
        @options.app.qualityProfiles

    serializeData: ->
      action: @action

      paging: @options.app.codingRules.paging
      qualityProfiles: @options.app.qualityProfiles

      qualityProfile: @options.app.getQualityProfile()
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()

      availableQualityProfiles: @getAvailableQualityProfiles()

      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
