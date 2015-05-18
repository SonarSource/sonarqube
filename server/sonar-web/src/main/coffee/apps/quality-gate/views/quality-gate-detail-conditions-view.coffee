#
# SonarQube, open source software quality management tool.
# Copyright (C) 2008-2014 SonarSource
# mailto:contact AT sonarsource DOT com
#
# SonarQube is free software; you can redistribute it and/or
# modify it under the terms of the GNU Lesser General Public
# License as published by the Free Software Foundation; either
# version 3 of the License, or (at your option) any later version.
#
# SonarQube is distributed in the hope that it will be useful,
# but WITHOUT ANY WARRANTY; without even the implied warranty of
# MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
# Lesser General Public License for more details.
#
# You should have received a copy of the GNU Lesser General Public License
# along with this program; if not, write to the Free Software Foundation,
# Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
#

define [
  '../models/condition',
  './quality-gate-detail-condition-view',
  './quality-gate-detail-conditions-empty-view'
  '../templates'
], (
  Condition,
  QualityGateDetailConditionView,
  QualityGateDetailConditionsEmptyView,
) ->

  class QualityGateDetailConditionsView extends Marionette.CompositeView
    template: Templates['quality-gate-detail-conditions']
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
      metrics = _.filter metrics, (metric) ->
        !metric.hidden
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
