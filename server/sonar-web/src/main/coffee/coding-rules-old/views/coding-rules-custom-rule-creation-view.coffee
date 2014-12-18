define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesCustomRuleCreationView extends Marionette.ItemView
    className: 'coding-rules-modal'
    template: Templates['coding-rules-custom-rule-creation']


    ui:
      customRuleCreationKey: '#coding-rules-custom-rule-creation-key'
      customRuleCreationName: '#coding-rules-custom-rule-creation-name'
      customRuleCreationHtmlDescription: '#coding-rules-custom-rule-creation-html-description'
      customRuleCreationSeverity: '#coding-rules-custom-rule-creation-severity'
      customRuleCreationStatus: '#coding-rules-custom-rule-creation-status'
      customRuleCreationParameters: '[name]'
      customRuleCreationCreate: '#coding-rules-custom-rule-creation-create'
      customRuleCreationReactivate: '#coding-rules-custom-rule-creation-reactivate'
      modalFoot: '.modal-foot'


    events:
      'input @ui.customRuleCreationName': 'generateKey'
      'keydown @ui.customRuleCreationName': 'generateKey'
      'keyup @ui.customRuleCreationName': 'generateKey'

      'input @ui.customRuleCreationKey': 'flagKey'
      'keydown @ui.customRuleCreationKey': 'flagKey'
      'keyup @ui.customRuleCreationKey': 'flagKey'

      'click #coding-rules-custom-rule-creation-cancel': 'hide'
      'click @ui.customRuleCreationCreate': 'create'
      'click @ui.customRuleCreationReactivate': 'reactivate'


    generateKey: ->
      unless @keyModifiedByUser
        if @ui.customRuleCreationKey
          generatedKey = @ui.customRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_')
          @ui.customRuleCreationKey.val generatedKey

    flagKey: ->
      @keyModifiedByUser = true
      # Cannot use @ui.customRuleCreationReactivate.hide() directly since it was not there at initial render
      jQuery(@ui.customRuleCreationReactivate.selector).hide()


    create: ->
      action = 'create'
      if @model and @model.has 'key'
        action = 'update'

      postData =
        name: @ui.customRuleCreationName.val()
        markdown_description: @ui.customRuleCreationHtmlDescription.val()
        severity: @ui.customRuleCreationSeverity.val()
        status: @ui.customRuleCreationStatus.val()

      if @model && @model.has 'key'
        postData.key = @model.get 'key'
      else
        postData.template_key = @templateRule.get 'key'
        postData.custom_key = @ui.customRuleCreationKey.val()
        postData.prevent_reactivation = true

      params = @ui.customRuleCreationParameters.map(->
        node = jQuery(@)
        value = node.val()
        if !value and action == 'create'
          value = node.prop('placeholder') || ''
        key: node.prop('name'), value: value).get()

      postData.params = (params.map (param) -> param.key + '=' + window.csvEscape(param.value)).join(';')
      @sendRequest(action, postData)


    reactivate: ->
      postData =
        name: @existingRule.name
        markdown_description: @existingRule.mdDesc
        severity: @existingRule.severity
        status: @existingRule.status
        template_key: @existingRule.templateKey
        custom_key: @ui.customRuleCreationKey.val()
        prevent_reactivation: false

      params = @existingRule.params
      postData.params = (params.map (param) -> param.key + '=' + param.defaultValue).join(';')

      @sendRequest('create', postData)


    sendRequest: (action, postData) ->
      @$('.modal-error').hide()
      @$('.modal-warning').hide()

      origFooter = @ui.modalFoot.html()
      @ui.modalFoot.html '<i class="spinner"></i>'

      jQuery.ajax
        type: 'POST'
        url: "#{baseUrl}/api/rules/" + action
        data: postData
        error: () ->
      .done (r) =>
          delete @templateRule
          @options.app.showRule r.rule.key
          @hide()
      .fail (jqXHR, textStatus, errorThrown) =>
          if jqXHR.status == 409
            @existingRule = jqXHR.responseJSON.rule
            @$('.modal-warning').show()
            @ui.modalFoot.html Templates['coding-rules-custom-rule-reactivation'](@)
          else
            jQuery.ajaxSettings.error(jqXHR, textStatus, errorThrown)
            @ui.modalFoot.html origFooter


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
      params = {}
      if @templateRule
        params = @templateRule.get 'params'
      else if @model and @model.has 'params'
        params = @model.get('params').map (p) ->
          _.extend p,
            value: p.defaultValue

      _.extend super,
        change: @model && @model.has 'key'
        params: params
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']
        statuses: _.map @options.app.statuses, (value, key) ->
          id: key
          text: value
