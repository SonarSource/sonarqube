import $ from 'jquery';
import _ from 'underscore';
import BaseFilters from './base-filters';
import ChoiceFilters from './choice-filters';
import Template from '../templates/favorite-filter.hbs';
import DetailsTemplate from '../templates/favorite-details-filter.hbs';

var DetailsFavoriteFilterView = BaseFilters.DetailsFilterView.extend({
  template: DetailsTemplate,


  events: {
    'click label[data-id]': 'applyFavorite',
    'click .manage label': 'manage'
  },


  applyFavorite: function (e) {
    var id = $(e.target).data('id');
    window.location = baseUrl + this.model.get('favoriteUrl') + '/' + id;
  },


  manage: function () {
    window.location = baseUrl + this.model.get('manageUrl');
  },


  serializeData: function () {
    var choices = this.model.get('choices'),
        choicesArray =
            _.sortBy(
                _.map(choices, function (v, k) {
                  return { v: v, k: k };
                }),
                'v');

    return _.extend({}, this.model.toJSON(), {
      choicesArray: choicesArray
    });
  }

});


var FavoriteFilterView = ChoiceFilters.ChoiceFilterView.extend({
  template: Template,
  className: 'navigator-filter navigator-filter-favorite',


  initialize: function () {
    ChoiceFilters.ChoiceFilterView.prototype.initialize.call(this, {
      detailsView: DetailsFavoriteFilterView
    });
  },


  renderValue: function () {
    return '';
  },


  renderInput: function () {
  },


  isDefaultValue: function () {
    return false;
  }

});


/*
 * Export public classes
 */

export default {
  DetailsFavoriteFilterView: DetailsFavoriteFilterView,
  FavoriteFilterView: FavoriteFilterView
};


