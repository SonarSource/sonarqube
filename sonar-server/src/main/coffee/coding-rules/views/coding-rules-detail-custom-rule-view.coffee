define [
  'backbone.marionette'
  'templates/coding-rules'
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
      if confirm('Are you sure ?')
        origEl = @$el.html()
        @$el.html '<i class="spinner"></i>'

        jQuery.ajax
          type: 'POST'
          url: "#{baseUrl}/api/rules/delete"
          data:
            key: @model.get 'key'
        .done =>
          @options.app.showRule @options.templateRule.get 'key'
        .fail =>
          @$el.html origEl

    serializeData: ->
      _.extend super,
        templateRule: @options.templateRule
        canWrite: @options.app.canWrite
