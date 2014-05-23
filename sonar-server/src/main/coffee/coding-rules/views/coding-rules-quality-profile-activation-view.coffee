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
      profileKey = @ui.qualityProfileSelect.val()
      params = @ui.qualityProfileParameters.map(->
        key: jQuery(@).prop('name'), value: jQuery(@).val() || jQuery(@).prop('placeholder')).get()

      paramsHash = _.reduce((params.map (param) -> param.key + '=' + param.value), ((hash, param) -> hash + ';' + param), '')

      if @model
        profileKey = @model.get('qProfile')
        unless profileKey
          profileKey = @model.get('key')
      severity = @ui.qualityProfileSeverity.val()

      @$('.modal-foot').html '<i class="spinner"></i>'
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualityprofiles/activate_rule"
        data:
            profile_key: profileKey
            rule_key: @rule.get('key')
            severity: severity
            params: paramsHash
      .done =>
          if @model
            @model.set severity: severity, params: params
          else
            model = new Backbone.Model
              name: _.findWhere(@options.app.qualityProfiles, key: profileKey).name
              key: profileKey
              severity: severity
              params: params
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
      params = @rule.get 'params'
      if @model
        modelParams = @model.get 'params'
        if modelParams
          params = params.map (p) ->
            _.extend p, value: _.findWhere(modelParams, key: p.key).value

      _.extend super,
        rule: @rule.toJSON()
        change: @model && @model.has 'severity'
        params: params
        qualityProfiles: @getAvailableQualityProfiles()
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
