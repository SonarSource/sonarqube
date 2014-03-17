define [
  'backbone.marionette',
  'common/handlebars-extensions'
], (
  Marionette
) ->

  class CodingRulesDetailQualityProfilesView extends Marionette.ItemView
    className: 'coding-rules-detail-quality-profile'
    template: getTemplate '#coding-rules-detail-quality-profile-template'


    ui:
      severitySelect: '.coding-rules-detail-quality-profile-severity'


    onRender: ->
      format = (state) ->
        return state.text unless state.id
        "<i class='icon-severity-#{state.id.toLowerCase()}'></i> #{state.text}"

      @ui.severitySelect.val @model.get 'severity'
      @ui.severitySelect.select2
        width: '200px'
        minimumResultsForSearch: 999
        formatResult: format
        formatSelection: format
        escapeMarkup: (m) -> m


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
        severities: ['BLOCKER', 'CRITICAL', 'MAJOR', 'MINOR', 'INFO']