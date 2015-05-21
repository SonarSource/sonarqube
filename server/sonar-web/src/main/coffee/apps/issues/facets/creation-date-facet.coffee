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
  './base-facet'
  '../templates'
], (
  BaseFacet
) ->

  $ = jQuery


  class extends BaseFacet
    template: Templates['issues-creation-date-facet']


    events: ->
      _.extend super,
        'change input': 'applyFacet'
        'click .js-select-period-start': 'selectPeriodStart'
        'click .js-select-period-end': 'selectPeriodEnd'

        'click .sonar-d3 rect': 'selectBar'

        'click .js-all': 'onAllClick'
        'click .js-last-week': 'onLastWeekClick'
        'click .js-last-month': 'onLastMonthClick'
        'click .js-last-year': 'onLastYearClick'


    onRender: ->
      @$el.toggleClass 'search-navigator-facet-box-collapsed', !@model.get('enabled')

      @$('input').datepicker
        dateFormat: 'yy-mm-dd'
        changeMonth: true
        changeYear: true

      props = ['createdAfter', 'createdBefore', 'createdAt']
      query = @options.app.state.get 'query'
      props.forEach (prop) =>
        value = query[prop]
        @$("input[name=#{prop}]").val value if value?

      values = @model.getValues()
      unless _.isArray(values) && values.length > 0
        date = moment()
        values = []
        for i in [0..10]
          values.push count: 0, val: date.toDate().toString()
          date = date.subtract 1, 'days'
        values.reverse()
      @$('.js-barchart').barchart values


    selectPeriodStart: ->
      @$('.js-period-start').datepicker 'show'


    selectPeriodEnd: ->
      @$('.js-period-end').datepicker 'show'


    applyFacet: ->
      obj = createdAt: null, createdInLast: null
      @$('input').each ->
        property = $(@).prop 'name'
        value = $(@).val()
        obj[property] = value
      @options.app.state.updateFilter obj


    disable: ->
      @options.app.state.updateFilter
        createdAfter: null
        createdBefore: null
        createdAt: null
        createdInLast: null


    selectBar: (e) ->
      periodStart = $(e.currentTarget).data 'period-start'
      periodEnd = $(e.currentTarget).data 'period-end'
      @options.app.state.updateFilter
        createdAfter: periodStart
        createdBefore: periodEnd
        createdAt: null
        createdInLast: null


    selectPeriod: (period) ->
      @options.app.state.updateFilter
        createdAfter: null
        createdBefore: null
        createdAt: null
        createdInLast: period


    onAllClick: ->
      @disable()


    onLastWeekClick: (e) ->
      e.preventDefault()
      @selectPeriod '1w'


    onLastMonthClick: (e) ->
      e.preventDefault()
      @selectPeriod '1m'


    onLastYearClick: (e) ->
      e.preventDefault()
      @selectPeriod '1y'


    serializeData: ->
      _.extend super,
        periodStart: @options.app.state.get('query').createdAfter
        periodEnd: @options.app.state.get('query').createdBefore
        createdAt: @options.app.state.get('query').createdAt
        createdInLast: @options.app.state.get('query').createdInLast
