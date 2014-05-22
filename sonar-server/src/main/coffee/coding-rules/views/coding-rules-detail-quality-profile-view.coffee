define [
  'backbone.marionette',
  'templates/coding-rules'
], (
  Marionette,
  Templates
) ->

  class CodingRulesDetailQualityProfileView extends Marionette.ItemView
    className: 'coding-rules-detail-quality-profile'
    template: Templates['coding-rules-detail-quality-profile']


    modelEvents:
      'change': 'render'


    ui:
      change: '.coding-rules-detail-quality-profile-change'
      revert: '.coding-rules-detail-quality-profile-revert'
      deactivate: '.coding-rules-detail-quality-profile-deactivate'


    events:
      'click @ui.change': 'change'
      'click @ui.revert': 'revert'
      'click @ui.deactivate': 'deactivate'


    initialize: ->
      super
      @model.set _.findWhere(@options.app.qualityProfiles, key: @model.get('qProfile'))

    change: ->
      @options.app.codingRulesQualityProfileActivationView.model = @model
      @options.app.codingRulesQualityProfileActivationView.show()


    revert: ->
      if confirm t 'are_you_sure'
        parent = @getParent()
        parameters = @model.get('parameters').map (p) ->
          _.extend {}, p, value: _.findWhere(parent.parameters, key: p.key).value
        @model.set 'parameters', parameters


    deactivate: ->
      if confirm t 'are_you_sure'
        jQuery.ajax
          type: 'POST'
          url: "#{baseUrl}/api/qualityprofiles/deactivate_rule"
          data:
            profile_key: @model.get('qProfile')
            rule_key: @options.rule.get('key')
        .done =>
          @model.destroy()


    enableUpdate: ->
      @ui.update.prop 'disabled', false


    getParent: ->
      return null if @model.get('inherit') == 'NONE'
      _.findWhere(@options.qualityProfiles, key: @model.get('inherit')).toJSON()


    enhanceParameters: ->
      parent = @getParent()
      parameters = @model.get 'parameters'
      return parameters unless parent
      parameters.map (p) ->
        _.extend p, original: _.findWhere(parent.parameters, key: p.key).value


    serializeData: ->
      _.extend super,
        parent: @getParent()
        parameters: @enhanceParameters()
        canWrite: @options.app.canWrite
