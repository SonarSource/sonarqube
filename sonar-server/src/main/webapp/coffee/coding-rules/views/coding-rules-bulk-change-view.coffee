define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesBulkChangeView extends Marionette.ItemView
    className: 'modal'
    template: getTemplate '#coding-rules-bulk-change-template'


    events:
      'submit form': 'onSubmit'
      'click #coding-rules-cancel-bulk-change': 'hide'
      'click label': 'enableAction'
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

      @$('#coding-rules-bulk-change-activate-on, #coding-rules-bulk-change-deactivate-on').select2
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


    show: ->
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    prepareQuery: ->
      query = @options.app.getQuery()
      activateIn = []
      deactivateIn = []
      severity = null
      if @$('#coding-rules-bulk-change-activate-qp').is(':checked')
        activateIn.push @options.app.getInactiveQualityProfile()
      if @$('#coding-rules-bulk-change-activate').is(':checked')
        activateIn.push @$('#coding-rules-bulk-change-activate-on').val()
      if @$('#coding-rules-bulk-change-deactivate-qp').is(':checked')
        deactivateIn.push @options.app.getActiveQualityProfile()
      if @$('#coding-rules-bulk-change-deactivate').is(':checked')
        deactivateIn.push @$('#coding-rules-bulk-change-deactivate-on').val()
      if @$('#coding-rules-bulk-change-set-severity').is(':checked')
        severity = @$('#coding-rules-bulk-change-severity').val()
      actions = []
      if activateIn.length > 0
        actions.push 'bulk_activate'
        _.extend query, bulk_activate_in: activateIn.join(',')
      if deactivateIn.length > 0
        actions.push 'bulk_deactivate'
        _.extend query, bulk_deactivate_in: deactivateIn.join(',')
      if severity
        actions.push 'bulk_set_severity'
        _.extend query, bulk_severity: severity
      _.extend query, bulk_actions: actions

    onSubmit: (e) ->
      e.preventDefault()
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/bulk_change"
        data: @prepareQuery()
      .done =>
        @options.app.fetchFirstPage()
        @hide()



    enableAction: (e) ->
      jQuery(e.target).siblings('input[type=checkbox]').prop 'checked', true


    serializeData: ->
      paging: @options.app.codingRules.paging
      qualityProfiles: @options.app.qualityProfiles

      activeQualityProfile: @options.app.getActiveQualityProfile()
      activeQualityProfileName: @options.app.activeInFilter.view.renderValue()
      activateOnQualityProfiles: _.reject @options.app.qualityProfiles, (q) => q.key == @options.app.getInactiveQualityProfile()

      inactiveQualityProfile: @options.app.getInactiveQualityProfile()
      inactiveQualityProfileName: @options.app.inactiveInFilter.view.renderValue()
      deactivateOnQualityProfiles: _.reject @options.app.qualityProfiles, (q) => q.key == @options.app.getActiveQualityProfile()

      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
