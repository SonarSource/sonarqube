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
  './profile-view',
  './templates'
], function (ProfileView) {

  return Marionette.CompositeView.extend({
    className: 'list-group',
    template: Templates['quality-profiles-profiles'],
    languageTemplate: Templates['quality-profiles-profiles-language'],
    itemView: ProfileView,
    itemViewContainer: '.js-list',

    collectionEvents: {
      'filter': 'filterByLanguage'
    },

    itemViewOptions: function (model) {
      return {
        collectionView: this,
        highlighted: model.get('key') === this.highlighted
      };
    },

    highlight: function (key) {
      this.highlighted = key;
      this.render();
    },

    appendHtml: function (compositeView, itemView, index) {
      var $container = this.getItemViewContainer(compositeView),
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
      return $container.append(itemView.el);
    },

    closeChildren: function () {
      Marionette.CompositeView.prototype.closeChildren.apply(this, arguments);
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

});
