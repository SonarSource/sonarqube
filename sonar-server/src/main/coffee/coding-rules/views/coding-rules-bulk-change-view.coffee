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

    ui:
      modalFooter: '.modal-foot'
      modalError: '.modal-error'
      modalWarning: '.modal-warning'
      modalNotice: '.modal-notice'
      codingRulesSubmitBulkChange: '#coding-rules-submit-bulk-change'
      codingRulesCancelBulkChange: '#coding-rules-cancel-bulk-change'
      codingRulesCloseBulkChange: '#coding-rules-close-bulk-change'

    events:
      'submit form': 'onSubmit'
      'click @ui.codingRulesCancelBulkChange': 'hide'
      'click @ui.codingRulesCloseBulkChange': 'close'
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


    show: (action, param = null) ->
      @action = action
      @profile = param
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    close: ->
      @options.app.fetchFirstPage(true)
      @hide()


    prepareQuery: ->
      query = @options.app.getQuery()

      if @action == 'activate' || @action == 'deactivate'
        _.extend query,
          wsAction: @action
          profile_key: @$('#coding-rules-bulk-change-profile').val() or @profile

      if @action == 'change-severity'
        _.extend query,
          wsAction: 'activate'
          profile_key: @profile
          activation_severity: @$('#coding-rules-bulk-change-severity').val()

      query


    bulkChange: (query) ->
      wsAction = query.wsAction
      query = _.omit(query, 'wsAction')

      @ui.modalError.hide()
      @ui.modalWarning.hide()
      @ui.modalNotice.hide()

      origFooter = @ui.modalFooter.html()
      @ui.modalFooter.html '<i class="spinner"></i>'

      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualityprofiles/#{wsAction}_rules"
        data: query
      .done (r) =>
        if (r.failed)
          @ui.modalWarning.show()
          @ui.modalWarning.html tp('coding_rules.bulk_change.warning', r.succeeded, r.failed)
        else
          @ui.modalNotice.show()
          @ui.modalNotice.html tp('coding_rules.bulk_change.success', r.succeeded)

        @ui.modalFooter.html origFooter
        @$(@ui.codingRulesSubmitBulkChange.selector).hide()
        @$(@ui.codingRulesCancelBulkChange.selector).hide()
        @$(@ui.codingRulesCloseBulkChange.selector).show()
      .fail =>
        @ui.modalFooter.html origFooter


    onSubmit: (e) ->
      e.preventDefault()
      @bulkChange(@prepareQuery())


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

      qualityProfile: @profile
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()

      availableQualityProfiles: @getAvailableQualityProfiles()

      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
