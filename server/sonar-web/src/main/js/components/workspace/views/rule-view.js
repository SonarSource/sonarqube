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
import { union } from 'lodash';
import Marionette from 'backbone.marionette';
import BaseView from './base-viewer-view';
import Template from '../templates/workspace-rule.hbs';
import { getPathUrlAsString, getRulesUrl } from '../../../helpers/urls';
import { areThereCustomOrganizations } from '../../../store/organizations/utils';

export default BaseView.extend({
  template: Template,

  onRender() {
    BaseView.prototype.onRender.apply(this, arguments);
    this.$('[data-toggle="tooltip"]').tooltip({ container: 'body' });
  },

  onDestroy() {
    this.$('[data-toggle="tooltip"]').tooltip('destroy');
  },

  serializeData() {
    const query = { rule_key: this.model.get('key') };
    const permalink = getPathUrlAsString(
      areThereCustomOrganizations()
        ? getRulesUrl(query, this.model.get('organization'))
        : getRulesUrl(query, undefined)
    );

    return {
      ...Marionette.LayoutView.prototype.serializeData.apply(this, arguments),
      allTags: union(this.model.get('sysTags'), this.model.get('tags')),
      permalink
    };
  }
});
