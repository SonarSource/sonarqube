/*
 * SonarQube
 * Copyright (C) 2009-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
import $ from 'jquery';
import _ from 'underscore';
import BaseFilters from './base-filters';
import ChoiceFilters from './choice-filters';
import Template from '../templates/favorite-filter.hbs';
import DetailsTemplate from '../templates/favorite-details-filter.hbs';

const DetailsFavoriteFilterView = BaseFilters.DetailsFilterView.extend({
  template: DetailsTemplate,


  events: {
    'click label[data-id]': 'applyFavorite',
    'click .manage label': 'manage'
  },


  applyFavorite (e) {
    const id = $(e.target).data('id');
    window.location = this.model.get('favoriteUrl') + '/' + id;
  },


  manage () {
    window.location = this.model.get('manageUrl');
  },


  serializeData () {
    const choices = this.model.get('choices');
    const choicesArray =
        _.sortBy(
            _.map(choices, function (v, k) {
              return { v, k };
            }),
            'v');

    return _.extend({}, this.model.toJSON(), {
      choicesArray
    });
  }

});


const FavoriteFilterView = ChoiceFilters.ChoiceFilterView.extend({
  template: Template,
  className: 'navigator-filter navigator-filter-favorite',


  initialize () {
    ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
      projectsView: DetailsFavoriteFilterView
    });
  },


  renderValue () {
    return '';
  },


  renderInput () {
  },


  isDefaultValue () {
    return false;
  }

});


/*
 * Export public classes
 */

export default {
  DetailsFavoriteFilterView,
  FavoriteFilterView
};


