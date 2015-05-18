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
    './base-facet'
], function (BaseFacet) {

  var $ = jQuery;

  return BaseFacet.extend({

    initialize: function (options) {
      this.listenTo(options.app.state, 'change:query', this.onQueryChange);
    },

    onQueryChange: function () {
      var query = this.options.app.state.get('query'),
          isProfileSelected = query.qprofile != null;
      if (isProfileSelected) {
        var profile = _.findWhere(this.options.app.qualityProfiles, { key: query.qprofile });
        if (profile != null && profile.parentKey == null) {
          this.forbid();
        }
      } else {
        this.forbid();
      }
    },

    onRender: function () {
      BaseFacet.prototype.onRender.apply(this, arguments);
      this.onQueryChange();
    },

    forbid: function () {
      BaseFacet.prototype.forbid.apply(this, arguments);
      this.$el.prop('title', t('coding_rules.filters.inheritance.inactive'));
    },

    allow: function () {
      BaseFacet.prototype.allow.apply(this, arguments);
      this.$el.prop('title', null);
    },

    getValues: function () {
      var values = ['NONE', 'INHERITED', 'OVERRIDES'];
      return values.map(function (key) {
        return {
          label: t('coding_rules.filters.inheritance', key.toLowerCase()),
          val: key
        };
      });
    },

    toggleFacet: function (e) {
      var obj = {},
          property = this.model.get('property');
      if ($(e.currentTarget).is('.active')) {
        obj[property] = null;
      } else {
        obj[property] = $(e.currentTarget).data('value');
      }
      this.options.app.state.updateFilter(obj);
    },

    serializeData: function () {
      return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
        values: this.getValues()
      });
    }
  });

});
