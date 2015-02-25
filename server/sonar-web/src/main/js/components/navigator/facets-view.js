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
  'components/navigator/facets/base-facet'
], function (BaseFacet) {

  return Marionette.CollectionView.extend({
    className: 'search-navigator-facets-list',

    itemViewOptions: function () {
      return {
        app: this.options.app
      };
    },

    getItemView: function () {
      return BaseFacet;
    },

    collectionEvents: function () {
      return {
        'change:enabled': 'updateState'
      };
    },

    updateState: function () {
      var enabledFacets = this.collection.filter(function (model) {
            return model.get('enabled');
          }),
          enabledFacetIds = enabledFacets.map(function (model) {
            return model.id;
          });
      this.options.app.state.set({facets: enabledFacetIds});
    }

  });

});
