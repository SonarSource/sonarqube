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
  'widgets/issue-filter'
  'templates/issues'
], (IssueFilter) ->

  $ = jQuery


  Handlebars.registerHelper 'issuesHomeLink', (property, value) ->
    "#{baseUrl}/issues/search#resolved=false|createdInLast=1w|#{property}=#{encodeURIComponent value}"

  Handlebars.registerHelper 'myIssuesHomeLink', (property, value) ->
    "#{baseUrl}/issues/search#resolved=false|createdInLast=1w|assignees=__me__|#{property}=#{encodeURIComponent value}"

  Handlebars.registerHelper 'issueFilterHomeLink', (id) ->
    "#{baseUrl}/issues/search#id=#{id}"


  class extends Marionette.ItemView
    template: Templates['issues-workspace-home']


    modelEvents:
      'change': 'render'


    events:
      'click .js-barchart rect': 'selectBar'
      'click .js-my-barchart rect': 'selectMyBar'


    initialize: ->
      @model = new Backbone.Model
      @requestIssues()
      @requestMyIssues()


    _getProjects: (r) ->
      projectFacet = _.findWhere r.facets, property: 'projectUuids'
      if projectFacet?
        values = _.head projectFacet.values, 3
        values.forEach (v) =>
          project = _.findWhere r.projects, uuid: v.val
          v.label = project.longName
        values


    _getAuthors: (r) ->
      authorFacet = _.findWhere r.facets, property: 'authors'
      if authorFacet?
        values = _.head authorFacet.values, 3
        values


    _getTags: (r) ->
      MIN_SIZE = 10
      MAX_SIZE = 24
      tagFacet = _.findWhere r.facets, property: 'tags'
      if tagFacet?
        values = _.head tagFacet.values, 10
        minCount = _.min(values, (v) -> v.count).count
        maxCount = _.max(values, (v) -> v.count).count
        scale = d3.scale.linear().domain([minCount, maxCount]).range([MIN_SIZE, MAX_SIZE])
        values.forEach (v) =>
          v.size = scale v.count
        values


    requestIssues: ->
      url = "#{baseUrl}/api/issues/search"
      options =
        resolved: false
        createdInLast: '1w'
        ps: 1
        facets: 'createdAt,projectUuids,authors,tags'
      $.get(url, options).done (r) =>
        @model.set
          createdAt: _.findWhere(r.facets, property: 'createdAt')?.values
          projects: @_getProjects r
          authors: @_getAuthors r
          tags: @_getTags r


    requestMyIssues: ->
      url = "#{baseUrl}/api/issues/search"
      options =
        resolved: false
        createdInLast: '1w'
        assignees: '__me__'
        ps: 1
        facets: 'createdAt,projectUuids,authors,tags'
      $.get(url, options).done (r) =>
        @model.set
          myCreatedAt: _.findWhere(r.facets, property: 'createdAt')?.values
          myProjects: @_getProjects r
          myTags: @_getTags r


    onRender: ->
      values = @model.get 'createdAt'
      myValues = @model.get 'myCreatedAt'
      @$('.js-barchart').barchart values if values?
      @$('.js-my-barchart').barchart myValues if myValues?
      @$('[data-toggle="tooltip"]').tooltip container: 'body'


    selectBar: (e) ->
      periodStart = $(e.currentTarget).data 'period-start'
      periodEnd = $(e.currentTarget).data 'period-end'
      @options.app.state.setQuery
        resolved: false
        createdAfter: periodStart
        createdBefore: periodEnd


    selectMyBar: (e) ->
      periodStart = $(e.currentTarget).data 'period-start'
      periodEnd = $(e.currentTarget).data 'period-end'
      @options.app.state.setQuery
        resolved: false
        assignees: '__me__'
        createdAfter: periodStart
        createdBefore: periodEnd


    serializeData: ->
      _.extend super,
        user: window.SS.user
        filters: _.sortBy @options.app.filters.toJSON(), 'name'
