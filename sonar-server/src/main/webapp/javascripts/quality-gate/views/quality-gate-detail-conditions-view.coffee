define [
  'backbone.marionette',
  'handlebars',
  'quality-gate/models/condition',
  'quality-gate/views/quality-gate-detail-condition-view',
  'quality-gate/views/quality-gate-detail-conditions-empty-view'
], (
  Marionette,
  Handlebars,
  Condition,
  QualityGateDetailConditionView,
  QualityGateDetailConditionsEmptyView
) ->

  class QualityGateDetailConditionsView extends Marionette.CompositeView
    template: Handlebars.compile jQuery('#quality-gate-detail-conditions-template').html()
    itemView: QualityGateDetailConditionView
    emptyView: QualityGateDetailConditionsEmptyView
    itemViewContainer: '.quality-gate-conditions tbody'


    ui:
      metricSelect: '#quality-gate-new-condition-metric'
      introductionShowMore: '.quality-gate-introduction-show-more'
      introductionMore: '.quality-gate-introduction-more'


    events:
      'click @ui.introductionShowMore': 'showMoreIntroduction'
      'change @ui.metricSelect': 'addCondition'


    itemViewOptions: ->
      app: @options.app
      collectionView: @


    appendHtml: (compositeView, itemView) ->
      if (compositeView.isBuffering)
        compositeView.elBuffer.appendChild itemView.el
        compositeView._bufferedChildren.push itemView
      else
        container = @getItemViewContainer compositeView
        container.prepend itemView.el


    onRender: ->
      @ui.introductionMore.hide()
      @ui.metricSelect.select2
        allowClear: false,
        width: '250px',
        placeholder: t('alerts.select_metric')


    groupedMetrics: ->
      metrics = @options.app.metrics
      metrics = _.groupBy metrics, 'domain'
      metrics = _.map metrics, (metrics, domain) ->
        domain: domain, metrics: _.sortBy metrics, 'short_name'
      _.sortBy metrics, 'domain'


    serializeData: ->
      _.extend super,
        canEdit: @options.app.canEdit
        metricGroups: @groupedMetrics()


    showMoreIntroduction: ->
      @ui.introductionShowMore.hide()
      @ui.introductionMore.show()


    addCondition: ->
      metric = @ui.metricSelect.val()
      @ui.metricSelect.select2('val', '')
      condition = new Condition
        metric: metric
        gateId: @options.gateId
      @collection.unshift condition


    updateConditions: ->
      conditions = @collection.map (item) -> _.extend item.toJSON(),
        metric: item.get('metric').key
      @options.qualityGate.set { conditions: conditions }, { silent: true }
