define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesQualityProfileActivationView extends Marionette.ItemView
    className: 'modal'
    template: Templates['coding-rules-quality-profile-activation']


    ui:
      qualityProfileSelect: '#coding-rules-quality-profile-activation-select'
      qualityProfileSeverity: '#coding-rules-quality-profile-activation-severity'
      qualityProfileActivate: '#coding-rules-quality-profile-activation-activate'


    events:
      'click #coding-rules-quality-profile-activation-cancel': 'hide'
      'click @ui.qualityProfileActivate': 'activate'


    activate: ->
      @$('.modal-foot').html '<i class="spinner"></i>'
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/codingrules/activate"
        data: id: 1
      .done =>
        jQuery('.navigator-results-list .active').click()
        @hide()


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

      @ui.qualityProfileSelect.select2
        width: '250px'
        minimumResultsForSearch: 5

      format = (state) ->
        return state.text unless state.id
        "<i class='icon-severity-#{state.id.toLowerCase()}'></i> #{state.text}"

      @ui.qualityProfileSeverity.val @model.get 'severity'
      @ui.qualityProfileSeverity.select2
        width: '250px'
        minimumResultsForSearch: 999
        formatResult: format
        formatSelection: format


    show: ->
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    getAvailableQualityProfiles: ->
      _.reject @options.app.qualityProfiles, (profile) =>
        _.findWhere @model.get('qualityProfiles'), key: profile.key


    serializeData: ->
      _.extend super,
        qualityProfiles: @getAvailableQualityProfiles()
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
