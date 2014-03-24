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


    show: (action) ->
      @action = action
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    prepareQuery: ->
      query = @options.app.getQuery()
      switch @action
        when 'activate' then _.extend query, bulk_activate: @$('#coding-rules-bulk-change-activate-on').val()
        when 'deactivate' then _.extend query, bulk_deactivate: @$('#coding-rules-bulk-change-deactivate-on').val()
        when 'change-severity' then _.extend query, bulk_change_severity: @$('#coding-rules-bulk-change-severity').val()


    bulkChange: (query) ->
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/bulk_change"
        data: query
      .done =>
        @options.app.fetchFirstPage()

    onSubmit: (e) ->
      e.preventDefault()
      @bulkChange(@prepareQuery()).done => @hide()



    enableAction: (e) ->
      jQuery(e.target).siblings('input[type=checkbox]').prop 'checked', true


    serializeData: ->
      action: @action

      paging: @options.app.codingRules.paging
      qualityProfiles: @options.app.qualityProfiles

      qualityProfile: @options.app.getQualityProfile()
      qualityProfileName: @options.app.qualityProfileFilter.view.renderValue()

      activateOnQualityProfiles: @options.app.qualityProfiles
      deactivateOnQualityProfiles: _.reject @options.app.qualityProfiles, (q) => q.key == @options.app.getQualityProfile()

      severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
