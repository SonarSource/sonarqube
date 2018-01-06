/*
 * SonarQube
 * Copyright (C) 2009-2018 SonarSource SA
 * mailto:info AT sonarsource DOT com
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
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-inheritance-facet.hbs';
import { translate } from '../../../helpers/l10n';

export default BaseFacet.extend({
  template: Template,

  initialize(options) {
    this.listenTo(options.app.state, 'change:query', this.onQueryChange);
  },

  onQueryChange() {
    const query = this.options.app.state.get('query');
    const isProfileSelected = query.qprofile != null;
    if (isProfileSelected) {
      const profile = this.options.app.qualityProfiles.find(p => p.key === query.qprofile);
      if (profile != null && profile.parentKey == null) {
        this.forbid();
      }
    } else {
      this.forbid();
    }
  },

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    this.onQueryChange();
  },

  forbid() {
    BaseFacet.prototype.forbid.apply(this, arguments);
    this.$el.prop('title', translate('coding_rules.filters.inheritance.inactive'));
  },

  allow() {
    BaseFacet.prototype.allow.apply(this, arguments);
    this.$el.prop('title', null);
  },

  getValues() {
    const values = ['NONE', 'INHERITED', 'OVERRIDES'];
    return values.map(key => {
      return {
        label: translate('coding_rules.filters.inheritance', key.toLowerCase()),
        val: key
      };
    });
  },

  toggleFacet(e) {
    const obj = {};
    const property = this.model.get('property');
    if ($(e.currentTarget).is('.active')) {
      obj[property] = null;
    } else {
      obj[property] = $(e.currentTarget).data('value');
    }
    this.options.app.state.updateFilter(obj);
  },

  serializeData() {
    return {
      ...BaseFacet.prototype.serializeData.apply(this, arguments),
      values: this.getValues()
    };
  }
});
