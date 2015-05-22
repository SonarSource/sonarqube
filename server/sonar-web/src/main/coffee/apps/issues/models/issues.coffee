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
  'components/issue/models/issue'
], (
  Issue
) ->

  class extends Backbone.Collection
    model: Issue

    url: ->
      "#{baseUrl}/api/issues/search"


    # Used to parse /api/issues/search response
    parseIssues: (r) ->
      find = (source, key, keyField) ->
        searchDict = {}
        searchDict[keyField || 'key'] = key
        _.findWhere(source, searchDict) || key

      r.issues.map (issue, index) ->
        component = find r.components, issue.component
        project = find r.projects, issue.project
        subProject = find r.components, issue.subProject
        rule = find r.rules, issue.rule
        assignee = find r.users, issue.assignee, 'login'

        _.extend issue,
          index: index

        if component
          _.extend issue,
            componentUuid: component.uuid
            componentLongName: component.longName
            componentQualifier: component.qualifier

        if project
          _.extend issue,
            projectLongName: project.longName
            projectUuid: project.uuid

        if subProject
          _.extend issue,
            subProjectLongName: subProject.longName
            subProjectUuid: subProject.uuid

        if rule
          _.extend issue,
            ruleName: rule.name

        if assignee
          _.extend issue,
            assigneeEmail: assignee.email

        issue


    setIndex: ->
      @forEach (issue, index) ->
        issue.set index: index
