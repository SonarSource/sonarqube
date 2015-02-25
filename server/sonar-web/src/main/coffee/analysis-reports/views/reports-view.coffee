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
  'analysis-reports/views/report-view'
  'analysis-reports/views/reports-empty-view'
], (
  ReportView
  EmptyView
) ->

  $ = jQuery


  class extends Marionette.CollectionView
    tagName: 'ol'
    className: 'navigator-results-list'
    itemView: ReportView
    emptyView: EmptyView


    itemViewOptions: ->
      listView: @, app: @options.app


    initialize: ->
      @loadMoreThrottled = _.throttle @loadMore, 200


    onClose: ->
      @unbindScrollEvents()


    bindScrollEvents: ->
      $(window).on 'scroll', (=> @loadMoreThrottled())


    unbindScrollEvents: ->
      $(window).off 'scroll'


    loadMore: ->
      lastItem = this.children.findByIndex(@collection.length - 1)
      if $(window).scrollTop() + $(window).outerHeight() >= lastItem.$el.offset().top - 40
        @unbindScrollEvents()
        @options.app.fetchNextPage().done =>
          @bindScrollEvents() unless @collection.paging.maxResultsReached
