define [
  'backbone.marionette'
  'templates/coding-rules-old'
], (
  Marionette
  Templates
) ->

  class CodingRulesDetailCustomRuleView extends Marionette.ItemView
    tagName: 'tr'
    className: 'coding-rules-detail-custom-rule'
    template: Templates['coding-rules-detail-custom-rule']

    ui:
      delete: '.coding-rules-detail-custom-rule-delete'

    events:
      'click @ui.delete': 'delete'

    delete: ->
      confirmDialog
        title: t 'delete'
        html: t 'are_you_sure'
        yesHandler: =>
          origEl = @$el.html()
          @$el.html '<i class="spinner"></i>'

          jQuery.ajax
            type: 'POST'
            url: "#{baseUrl}/api/rules/delete"
            data:
              key: @model.get 'key'
          .done =>
            templateKey = @options.templateKey or @options.templateRule.get 'key'
            @options.app.showRule templateKey
          .fail =>
            @$el.html origEl

    serializeData: ->
      _.extend super,
        templateRule: @options.templateRule
        canWrite: @options.app.canWrite
