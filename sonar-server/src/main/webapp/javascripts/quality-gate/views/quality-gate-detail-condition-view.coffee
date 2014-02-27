define [
  'backbone.marionette',
  'handlebars'
], (
  Marionette,
  Handlebars
) ->

  class QualityGateDetailConditionView extends Marionette.ItemView
    tagName: 'tr'
    template: Handlebars.compile jQuery('#quality-gate-detail-condition-template').html()


    modelEvents:
      'change:id': 'render'


    ui:
      periodSelect: '[name=period]'
      operatorSelect: '[name=operator]'
      warningInput: '[name=warning]'
      errorInput: '[name=error]'
      actionsBox: '.quality-gate-condition-actions'


    events:
      'click .update-condition': 'saveCondition'
      'click .delete-condition': 'deleteCondition'
      'click .add-condition': 'saveCondition'
      'click .cancel-add-condition': 'cancelAddCondition'


    initialize: ->
      @populateMetric()


    populateMetric: ->
      metricKey = @model.get('metric')
      metric = @options.app.metrics.findWhere key: metricKey
      @model.set { metric: metric.toJSON() }, { silent: true }


    onRender: ->
      @ui.periodSelect.val @model.get('period') || '0'
      @ui.operatorSelect.val @model.get('op')

      @ui.periodSelect.select2
        allowClear: false
        minimumResultsForSearch: 999
        width: '200px'

      @ui.operatorSelect.select2
        allowClear: false
        minimumResultsForSearch: 999
        width: '150px'

      @ui.periodSelect.select2('open') if @model.isNew()


    serializeData: ->
      _.extend {}, @model.toJSON(),
        periods: window.SS.metricPeriods


    showSpinner: ->
      @ui.actionsBox.addClass 'navigator-fetching'


    hideSpinner: ->
      @ui.actionsBox.removeClass 'navigator-fetching'


    saveCondition: ->
      @showSpinner()
      @model.set
        period: @ui.periodSelect.val()
        op: @ui.operatorSelect.val()
        warning: @ui.warningInput.val()
        error: @ui.errorInput.val()
      @model.save()
        .always =>
          @hideSpinner()
        .done =>
          @options.collectionView.updateConditions()


    deleteCondition: ->
      if confirm window.SS.phrases.areYouSure
        @showSpinner()
        @model.delete().done =>
          @options.collectionView.updateConditions()
          @close()


    cancelAddCondition: ->
      @close()
