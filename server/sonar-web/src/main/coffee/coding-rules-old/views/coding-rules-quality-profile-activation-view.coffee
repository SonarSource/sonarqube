define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesQualityProfileActivationView extends Marionette.ItemView
    className: 'coding-rules-modal'
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
        key: jQuery(@).prop('name'), value: jQuery(@).val() || jQuery(@).prop('placeholder') || '').get()

      paramsHash = (params.map (param) -> param.key + '=' + window.csvEscape(param.value)).join(';')

      if @model
        profileKey = @model.get('qProfile')
        unless profileKey
          profileKey = @model.get('key')
      severity = @ui.qualityProfileSeverity.val()

      origFooter = @$('.modal-foot').html()
      @$('.modal-foot').html '<i class="spinner"></i>'

      ruleKey = @rule.get('key')
      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/qualityprofiles/activate_rule"
        data:
          profile_key: profileKey
          rule_key: ruleKey
          severity: severity
          params: paramsHash
      .done =>
          @options.app.showRule ruleKey
          @hide()
      .fail =>
          @$('.modal-foot').html origFooter


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


    getAvailableQualityProfiles: (lang) ->
      activeQualityProfiles =  @options.app.detailView.qualityProfilesView.collection
      inactiveProfiles = _.reject @options.app.qualityProfiles, (profile) =>
        activeQualityProfiles.findWhere key: profile.key
      _.filter inactiveProfiles, (profile) =>
        profile.lang == lang


    serializeData: ->
      params = @rule.get 'params'
      if @model
        modelParams = @model.get 'params'
        if modelParams
          params = params.map (p) ->
            parentParam = _.findWhere(modelParams, key: p.key)
            if parentParam
              _.extend p, value: _.findWhere(modelParams, key: p.key).value
            else
              p

      availableProfiles = @getAvailableQualityProfiles(@rule.get 'lang')

      _.extend super,
        rule: @rule.toJSON()
        change: @model && @model.has 'severity'
        params: params
        qualityProfiles: availableProfiles
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        saveEnabled: not _.isEmpty(availableProfiles) or (@model and @model.get('qProfile'))
        isCustomRule: (@model and @model.has('templateKey')) or @rule.has 'templateKey'
