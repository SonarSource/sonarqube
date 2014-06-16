define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesCustomRuleCreationView extends Marionette.ItemView
    className: 'modal'
    template: Templates['coding-rules-custom-rule-creation']


    ui:
      customRuleCreationKey: '#coding-rules-custom-rule-creation-key'
      customRuleCreationName: '#coding-rules-custom-rule-creation-name'
      customRuleCreationHtmlDescription: '#coding-rules-custom-rule-creation-html-description'
      customRuleCreationSeverity: '#coding-rules-custom-rule-creation-severity'
      customRuleCreationStatus: '#coding-rules-custom-rule-creation-status'
      customRuleCreationParameters: '[name]'
      customRuleCreationCreate: '#coding-rules-custom-rule-creation-create'


    events:
      'input @ui.customRuleCreationName': 'generateKey'
      'keydown @ui.customRuleCreationName': 'generateKey'
      'keyup @ui.customRuleCreationName': 'generateKey'

      'input @ui.customRuleCreationKey': 'flagKey'
      'keydown @ui.customRuleCreationKey': 'flagKey'
      'keyup @ui.customRuleCreationKey': 'flagKey'

      'click #coding-rules-custom-rule-creation-cancel': 'hide'
      'click @ui.customRuleCreationCreate': 'create'


    generateKey: ->
      unless @keyModifiedByUser
        if @ui.customRuleCreationKey
          generatedKey = @ui.customRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_')
          @ui.customRuleCreationKey.val generatedKey

    flagKey: ->
      @keyModifiedByUser = true


    create: ->
      action = 'create'

      postData =
        name: @ui.customRuleCreationName.val()
        html_description: @ui.customRuleCreationHtmlDescription.val()
        severity: @ui.customRuleCreationSeverity.val()
        status: @ui.customRuleCreationStatus.val()

      if @model && @model.has 'key'
        postData.key = @model.get 'key'
      else
        postData.template_key = @templateRule.get 'key'
        postData.custom_key = @ui.customRuleCreationKey.val()

      params = @ui.customRuleCreationParameters.map(->
        key: jQuery(@).prop('name'), value: jQuery(@).val() || jQuery(@).prop('placeholder') || '').get()

      postData.params = (params.map (param) -> param.key + '=' + param.value).join(';')

      origFooter = @$('.modal-foot').html()
      @$('.modal-foot').html '<i class="spinner"></i>'

      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/rules/" + action
        data: postData
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

      @keyModifiedByUser = false

      format = (state) ->
        return state.text unless state.id
        "<i class='icon-severity-#{state.id.toLowerCase()}'></i> #{state.text}"

      severity = (@model && @model.get 'severity') || @templateRule.get 'severity'
      @ui.customRuleCreationSeverity.val severity
      @ui.customRuleCreationSeverity.select2
        width: '250px'
        minimumResultsForSearch: 999
        formatResult: format
        formatSelection: format

      status = (@model && @model.get 'status') || @templateRule.get 'status'
      @ui.customRuleCreationStatus.val status
      @ui.customRuleCreationStatus.select2
        width: '250px'
        minimumResultsForSearch: 999


    show: ->
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    serializeData: ->
      params = @templateRule.get 'params'
      if @model
        modelParams = @model.get 'params'
        if modelParams
          params = params.map (p) ->
            parentParam = _.findWhere(modelParams, key: p.key)
            if parentParam
              _.extend p, value: _.findWhere(modelParams, key: p.key).value
            else
              p

      _.extend super,
        templateRule: @templateRule.toJSON()
        change: @model && @model.has 'key'
        params: params
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        statuses: _.map @options.app.statuses, (value, key) ->
          id: key
          text: value
