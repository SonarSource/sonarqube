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
  'components/navigator/facets-view'

  'issues/facets/base-facet'
  'issues/facets/severity-facet'
  'issues/facets/status-facet'
  'issues/facets/project-facet'
  'issues/facets/module-facet'
  'issues/facets/assignee-facet'
  'issues/facets/rule-facet'
  'issues/facets/tag-facet'
  'issues/facets/resolution-facet'
  'issues/facets/creation-date-facet'
  'issues/facets/action-plan-facet'
  'issues/facets/file-facet'
  'issues/facets/reporter-facet'
  'issues/facets/language-facet'
  'issues/facets/author-facet'
  'issues/facets/issue-key-facet'
  'issues/facets/context-facet'
], (
  FacetsView

  BaseFacet
  SeverityFacet
  StatusFacet
  ProjectFacet
  ModuleFacet
  AssigneeFacet
  RuleFacet
  TagFacet
  ResolutionFacet
  CreationDateFacet
  ActionPlanFacet
  FileFacet
  ReporterFacet
  LanguageFacet
  AuthorFacet
  IssueKeyFacet
  ContextFacet
) ->

  class extends FacetsView

    getItemView: (model) ->
      switch model.get 'property'
        when 'severities' then SeverityFacet
        when 'statuses' then StatusFacet
        when 'assignees' then AssigneeFacet
        when 'resolutions' then ResolutionFacet
        when 'createdAt' then CreationDateFacet
        when 'projectUuids' then ProjectFacet
        when 'moduleUuids' then ModuleFacet
        when 'rules' then RuleFacet
        when 'tags' then TagFacet
        when 'actionPlans' then ActionPlanFacet
        when 'fileUuids' then FileFacet
        when 'reporters' then ReporterFacet
        when 'languages' then LanguageFacet
        when 'authors' then AuthorFacet
        when 'issues' then IssueKeyFacet
        when 'context' then ContextFacet
        else BaseFacet
