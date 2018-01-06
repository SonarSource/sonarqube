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
import { sortBy } from 'lodash';
import BaseFacet from './base-facet';
import Template from '../templates/facets/coding-rules-severity-facet.hbs';
import { translate } from '../../../helpers/l10n';

export default BaseFacet.extend({
  template: Template,
  severities: ['BLOCKER', 'MINOR', 'CRITICAL', 'INFO', 'MAJOR'],

  initialize(options) {
    this.listenTo(options.app.state, 'change:query', this.onQueryChange);
  },

  onQueryChange() {
    const query = this.options.app.state.get('query');
    const isProfileSelected = query.qprofile != null;
    const isActiveShown = '' + query.activation === 'true';
    if (!isProfileSelected || !isActiveShown) {
      this.forbid();
    }
  },

  onRender() {
    BaseFacet.prototype.onRender.apply(this, arguments);
    this.onQueryChange();
  },

  forbid() {
    BaseFacet.prototype.forbid.apply(this, arguments);
    this.$el.prop('title', translate('coding_rules.filters.active_severity.inactive'));
  },

  allow() {
    BaseFacet.prototype.allow.apply(this, arguments);
    this.$el.prop('title', null);
  },

  sortValues(values) {
    const order = this.severities;
    return sortBy(values, v => order.indexOf(v.val));
  }
});
