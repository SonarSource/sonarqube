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
  'templates/analysis-reports'
], ->

  class extends Marionette.ItemView
    tagName: 'li'
    template: Templates['analysis-reports-report']


    onRender: ->
      status = @model.get 'status'
      @$el.addClass 'analysis-reports-report-pending' if status is 'PENDING'
      @$el.addClass 'analysis-reports-report-working' if status is 'WORKING'
      @$el.addClass 'analysis-reports-report-done' if status is 'SUCCESS'
      @$el.addClass 'analysis-reports-report-cancelled' if status is 'CANCELLED'
      @$el.addClass 'analysis-reports-report-failed' if status is 'FAIL'


    serializeData: ->
      duration = null
      if @model.has 'startedAt'
        startedAtMoment = moment @model.get 'startedAt'
        finishedAtMoment = moment(@model.get('finishedAt') || new Date())
        duration = finishedAtMoment.diff startedAtMoment
        duration =
          seconds: Math.floor (duration / 1000) % 60
          minutes: Math.floor (duration / (1000 * 60)) % 60
          hours: Math.floor (duration / (1000 * 60 * 60)) % 24
      _.extend super,
        duration: duration
