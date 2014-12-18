define [
  'backbone.marionette',
  'templates/coding-rules-old'
], (
  Marionette,
  Templates
) ->

  class CodingRulesManualRuleCreationView extends Marionette.ItemView
    template: Templates['coding-rules-manual-rule-creation']


    ui:
      manualRuleCreationKey: '#coding-rules-manual-rule-creation-key'
      manualRuleCreationName: '#coding-rules-manual-rule-creation-name'
      manualRuleCreationHtmlDescription: '#coding-rules-manual-rule-creation-html-description'
      manualRuleCreationSeverity: '#coding-rules-manual-rule-creation-severity'
      manualRuleCreationStatus: '#coding-rules-manual-rule-creation-status'
      manualRuleCreationParameters: '[name]'
      manualRuleCreationCreate: '#coding-rules-manual-rule-creation-create'
      manualRuleCreationReactivate: '#coding-rules-manual-rule-creation-reactivate'
      modalFoot: '.modal-foot'


    events:
      'input @ui.manualRuleCreationName': 'generateKey'
      'keydown @ui.manualRuleCreationName': 'generateKey'
      'keyup @ui.manualRuleCreationName': 'generateKey'

      'input @ui.manualRuleCreationKey': 'flagKey'
      'keydown @ui.manualRuleCreationKey': 'flagKey'
      'keyup @ui.manualRuleCreationKey': 'flagKey'

      'click #coding-rules-manual-rule-creation-cancel': 'hide'
      'click @ui.manualRuleCreationCreate': 'create'
      'click @ui.manualRuleCreationReactivate': 'reactivate'


    generateKey: ->
      unless @keyModifiedByUser
        if @ui.manualRuleCreationKey
          generatedKey = @ui.manualRuleCreationName.val().latinize().replace(/[^A-Za-z0-9]/g, '_')
          @ui.manualRuleCreationKey.val generatedKey

    flagKey: ->
      @keyModifiedByUser = true
      # Cannot use @ui.manualRuleCreationReactivate.hide() directly since it was not there at initial render
      jQuery(@ui.manualRuleCreationReactivate.selector).hide()


    create: ->
      action = 'create'
      if @model and @model.has 'key'
        action = 'update'

      postData =
        name: @ui.manualRuleCreationName.val()
        markdown_description: @ui.manualRuleCreationHtmlDescription.val()

      if @model && @model.has 'key'
        postData.key = @model.get 'key'
      else
        postData.manual_key = @ui.manualRuleCreationKey.val()
        postData.prevent_reactivation = true

      @sendRequest(action, postData)


    reactivate: ->
      postData =
        name: @existingRule.name
        markdown_description: @existingRule.mdDesc
        manual_key: @ui.manualRuleCreationKey.val()
        prevent_reactivation: false

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
          @options.app.showRule r.rule.key
          @hide()
      .fail (jqXHR, textStatus, errorThrown) =>
          if jqXHR.status == 409
            @existingRule = jqXHR.responseJSON.rule
            @$('.modal-warning').show()
            @ui.modalFoot.html Templates['coding-rules-manual-rule-reactivation'](@)
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


    show: ->
      @render()
      @$el.dialog 'open'


    hide: ->
      @$el.dialog 'close'


    serializeData: ->
      _.extend super,
        change: @model && @model.has 'key'
