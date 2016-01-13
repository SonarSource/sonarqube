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
import Marionette from 'backbone.marionette';
import ProfileView from './profile-view';
import ProfilesEmptyView from './profiles-empty-view';
import Template from './templates/quality-profiles-profiles.hbs';
import LanguageTemplate from './templates/quality-profiles-profiles-language.hbs';

export default Marionette.CompositeView.extend({
  className: 'list-group',
  template: Template,
  languageTemplate: LanguageTemplate,
  childView: ProfileView,
  childViewContainer: '.js-list',
  emptyView: ProfilesEmptyView,

  collectionEvents: {
    'filter': 'filterByLanguage'
  },

  childViewOptions: function (model) {
    return {
      collectionView: this,
      highlighted: model.get('key') === this.highlighted
    };
  },

  highlight: function (key) {
    this.highlighted = key;
    this.render();
  },

  attachHtml: function (compositeView, childView, index) {
    var $container = this.getChildViewContainer(compositeView),
        model = this.collection.at(index);
    if (model != null) {
      var prev = this.collection.at(index - 1),
          putLanguage = prev == null;
      if (prev != null) {
        var lang = model.get('language'),
            prevLang = prev.get('language');
        if (lang !== prevLang) {
          putLanguage = true;
        }
      }
      if (putLanguage) {
        $container.append(this.languageTemplate(model.toJSON()));
      }
    }
    compositeView._insertAfter(childView);
  },

  destroyChildren: function () {
    Marionette.CompositeView.prototype.destroyChildren.apply(this, arguments);
    this.$('.js-list-language').remove();
  },

  filterByLanguage: function (language) {
    if (language) {
      this.$('[data-language]').addClass('hidden');
      this.$('[data-language="' + language + '"]').removeClass('hidden');
    } else {
      this.$('[data-language]').removeClass('hidden');
    }
  }
});


