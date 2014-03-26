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
        @model.destroy()


    enableUpdate: ->
      @ui.update.prop 'disabled', false


    getParent: ->
      return null unless @model.get 'inherits'
      @options.qualityProfiles.findWhere(key: @model.get('inherits')).toJSON()


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