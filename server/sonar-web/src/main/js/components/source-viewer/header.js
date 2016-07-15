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
 /* @flow */
import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import MoreActionsView from './more-actions';
import MeasuresOverlay from './measures-overlay';
import Template from './templates/source-viewer-header.hbs';

const API_FAVORITE: string = window.baseUrl + '/api/favourites';

export default Marionette.ItemView.extend({
  template: Template,

  events () {
    return {
      'click .js-favorite': 'toggleFavorite',
      'click .js-actions': 'showMoreActions',
      'click .js-permalink': 'getPermalink'
    };
  },

  toggleFavorite () {
    const that = this;
    if (this.model.get('fav')) {
      $.ajax({
        url: API_FAVORITE + '/' + this.model.get('key'),
        type: 'DELETE'
      }).done(function () {
        that.model.set('fav', false);
        that.render();
      });
    } else {
      $.ajax({
        url: API_FAVORITE,
        type: 'POST',
        data: {
          key: this.model.get('key')
        }
      }).done(function () {
        that.model.set('fav', true);
        that.render();
      });
    }
  },

  showMoreActions (e) {
    e.stopPropagation();
    $('body').click();
    const view = new MoreActionsView({ parent: this });
    view.render().$el.appendTo(this.$el);
  },

  getPermalink () {
    let query = 'id=' + encodeURIComponent(this.model.get('key'));
    const windowParams = 'resizable=1,scrollbars=1,status=1';
    if (this.options.viewer.highlightedLine) {
      query = query + '&line=' + this.options.viewer.highlightedLine;
    }
    window.open(window.baseUrl + '/component/index?' + query, this.model.get('name'), windowParams);
  },

  showRawSources () {
    const url = window.baseUrl + '/api/sources/raw?key=' + encodeURIComponent(this.model.get('key'));
    const windowParams = 'resizable=1,scrollbars=1,status=1';
    window.open(url, this.model.get('name'), windowParams);
  },

  showMeasures () {
    new MeasuresOverlay({
      model: this.model,
      large: true
    }).render();
  },

  serializeData () {
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      path: this.model.get('path') || this.model.get('longName')
    });
  }
});
