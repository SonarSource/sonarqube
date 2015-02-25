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
  'templates/quality-gates'
], ->

  class QualityGateDetailConditionView extends Marionette.ItemView
    tagName: 'tr'
    template: Templates['quality-gate-detail-condition']
    spinner: '<i class="spinner"></i>'


    modelEvents:
      'change:id': 'render'


    ui:
      periodSelect: '[name=period]'
      operatorSelect: '[name=operator]'
      warningInput: '[name=warning]'
      errorInput: '[name=error]'
      actionsBox: '.quality-gate-condition-actions'
      updateButton: '.update-condition'
      deleteButton: '.delete-condition'


    events:
      'click @ui.updateButton': 'saveCondition'
      'click @ui.deleteButton': 'deleteCondition'
      'click .add-condition': 'saveCondition'
      'click .cancel-add-condition': 'cancelAddCondition'
      'keyup :input': 'enableUpdate'
      'change :input': 'enableUpdate'


    initialize: ->
      @populateMetric()


    populateMetric: ->
      metricKey = @model.get('metric')
      metric = _.findWhere @options.app.metrics, key: metricKey
      if metric?
        switch metric.type
          when 'WORK_DUR' then metric.placeholder = '1d 7h 59min'
          when 'RATING' then metric.placeholder = 'A'
      @model.set { metric: metric }, { silent: true }
      @model.set { isDiffMetric: metric.key.indexOf('new_') == 0 }, { silent: true }


    onRender: ->
      @ui.periodSelect.val @model.get('period') || '0'
      @ui.operatorSelect.val @model.get('op')
      @ui.warningInput.val @model.get('warning')
      @ui.errorInput.val @model.get('error')

      @ui.periodSelect.select2
        allowClear: false
        minimumResultsForSearch: 999
        width: '100%'

      @ui.operatorSelect.select2
        allowClear: false
        minimumResultsForSearch: 999
        width: '150px'

      @ui.periodSelect.select2('open') if @model.isNew()


    showSpinner: ->
      jQuery(@spinner).prependTo @ui.actionsBox
      @ui.actionsBox.find(':not(.spinner)').hide()


    hideSpinner: ->
      @ui.actionsBox.find('.spinner').remove()
      @ui.actionsBox.find(':not(.spinner)').show()


    saveCondition: ->
      @showSpinner()
      @model.set
        period: @ui.periodSelect.val()
        op: @ui.operatorSelect.val()
        warning: @ui.warningInput.val()
        error: @ui.errorInput.val()
      @model.save()
        .always =>
          @ui.updateButton.prop 'disabled', true
          @hideSpinner()
        .done =>
          @options.collectionView.updateConditions()


    deleteCondition: ->
      confirmDialog
        title: t 'quality_gates.delete_condition'
        html: tp 'quality_gates.delete_condition.confirm.message', @model.get('metric').name
        yesLabel: t 'delete'
        noLabel: t 'cancel'
        yesHandler: =>
          @showSpinner()
          @model.delete().done =>
            @options.collectionView.collection.remove @model
            @options.collectionView.updateConditions()
            @close()
        always: => @ui.deleteButton.blur()


    cancelAddCondition: ->
      @close()


    enableUpdate: ->
      @ui.updateButton.prop 'disabled', false


    serializeData: ->
      period = _.findWhere(@options.app.periods, key: this.model.get('period'))
      data = _.extend super,
        canEdit: @options.app.canEdit
        periods: @options.app.periods
        periodText: period?.text
      unless @options.app.canEdit
        _.extend data,
          warning: jQuery('<input>').data('type', @model.get('metric').type).val(@model.get('warning')).originalVal()
          error: jQuery('<input>').data('type', @model.get('metric').type).val(@model.get('error')).originalVal()
      data
