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
      qualityProfileParameters: '[name]'


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
          severity = @ui.qualityProfileSeverity.val()
          parameters = @ui.qualityProfileParameters.map(->
            key: jQuery(@).prop('name'), value: jQuery(@).val() || jQuery(@).prop('placeholder')).get()

          if @model
            @model.set severity: severity, parameters: parameters
          else
            key = @ui.qualityProfileSelect.val()
            model = new Backbone.Model
              name: _.findWhere(@options.app.qualityProfiles, key: key).name
              key: key
              severity: severity
              parameters: parameters
            @options.app.detailView.qualityProfilesView.collection.add model

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

      severity = (@model && @model.get 'severity') || @rule.get 'severity'
      @ui.qualityProfileSeverity.val severity
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
      activeQualityProfiles =  @options.app.detailView.qualityProfilesView.collection
      _.reject @options.app.qualityProfiles, (profile) =>
        activeQualityProfiles.findWhere key: profile.key


    serializeData: ->
      parameters = @rule.get 'parameters'
      if @model
        modelParameters = @model.get 'parameters'
        if modelParameters
          parameters = parameters.map (p) ->
            _.extend p, value: _.findWhere(modelParameters, key: p.key).value

      _.extend super,
        rule: @rule.toJSON()
        change: @model && @model.has 'severity'
        parameters: parameters
        qualityProfiles: @getAvailableQualityProfiles()
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
