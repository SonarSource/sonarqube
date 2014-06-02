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
      ruleKey = @options.rule.get('key')
      if confirm t 'are_you_sure'
        that = @
        jQuery.ajax
          type: 'POST'
          url: "#{baseUrl}/api/qualityprofiles/activate_rule"
          data:
              profile_key: @model.get('qProfile')
              rule_key: ruleKey
        .done =>
          @options.app.showRule ruleKey


    deactivate: ->
      ruleKey = @options.rule.get('key')
      if confirm t 'are_you_sure'
        jQuery.ajax
          type: 'POST'
          url: "#{baseUrl}/api/qualityprofiles/deactivate_rule"
          data:
            profile_key: @model.get('qProfile')
            rule_key: ruleKey
        .done =>
          @options.app.showRule ruleKey


    enableUpdate: ->
      @ui.update.prop 'disabled', false


    getParent: ->
      return null unless @model.get('inherit') && @model.get('inherit') != 'NONE'
      parentKey = @model.get('parent') + ':' + @model.get('lang')
      parent = _.extend {}, _.findWhere(@options.app.qualityProfiles, key: parentKey)
      _.extend parent, severity: @model.collection.findWhere(qProfile: parentKey).get 'severity'
      parent


    enhanceParameters: ->
      parent = @getParent()
      params = @model.get 'params'
      return params unless parent
      params.map (p) ->
        _.extend p, original: _.findWhere(parent.params, key: p.key).value


    serializeData: ->
      hash = _.extend super,
        parent: @getParent()
        parameters: @enhanceParameters()
        canWrite: @options.app.canWrite
