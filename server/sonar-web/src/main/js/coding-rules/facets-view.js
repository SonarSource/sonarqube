/*
 * SonarQube, open source software quality management tool.
 * Copyright (C) 2008-2014 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * SonarQube is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * SonarQube is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
define([
      'components/navigator/facets-view',
      'coding-rules/facets/base-facet',
      'coding-rules/facets/query-facet',
      'coding-rules/facets/key-facet',
      'coding-rules/facets/language-facet',
      'coding-rules/facets/repository-facet',
      'coding-rules/facets/tag-facet',
      'coding-rules/facets/quality-profile-facet',
      'coding-rules/facets/characteristic-facet',
      'coding-rules/facets/severity-facet',
      'coding-rules/facets/status-facet',
      'coding-rules/facets/available-since-facet',
      'coding-rules/facets/inheritance-facet',
      'coding-rules/facets/active-severity-facet',
      'coding-rules/facets/template-facet'
    ],
    function (FacetsView,
              BaseFacet,
              QueryFacet,
              KeyFacet,
              LanguageFacet,
              RepositoryFacet,
              TagFacet,
              QualityProfileFacet,
              CharacteristicFacet,
              SeverityFacet,
              StatusFacet,
              AvailableSinceFacet,
              InheritanceFacet,
              ActiveSeverityFacet,
              TemplateFacet) {

      var viewsMapping = {
        q: QueryFacet,
        rule_key: KeyFacet,
        languages: LanguageFacet,
        repositories: RepositoryFacet,
        tags: TagFacet,
        qprofile: QualityProfileFacet,
        debt_characteristics: CharacteristicFacet,
        severities: SeverityFacet,
        statuses: StatusFacet,
        available_since: AvailableSinceFacet,
        inheritance: InheritanceFacet,
        active_severities: ActiveSeverityFacet,
        is_template: TemplateFacet
      };

      return FacetsView.extend({

        getItemView: function (model) {
          var view = viewsMapping[model.get('property')];
          return view ? view : BaseFacet;
        }

      });

    });
