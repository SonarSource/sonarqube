/*
 * SonarQube
 * Copyright (C) 2009-2017 SonarSource SA
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
import Marionette from 'backbone.marionette';
import Template from './templates/facets/issues-my-issues-facet.hbs';

export default Marionette.ItemView.extend({
  template: Template,
  className: 'issues-header-inner',

  events: {
    'change [name="issues-page-my"]': 'onMyIssuesChange'
  },

  initialize() {
    this.listenTo(this.options.app.state, 'change:query', this.render);
  },

  onMyIssuesChange() {
    const mode = this.$('[name="issues-page-my"]:checked').val();
    if (mode === 'my') {
      this.options.app.state.updateFilter({
        assigned_to_me: 'true',
        assignees: null,
        assigned: null
      });
    } else {
      this.options.app.state.updateFilter({
        assigned_to_me: null,
        assignees: null,
        assigned: null
      });
    }
  },
  serializeData() {
    const me = !!this.options.app.state.get('query').assigned_to_me;
    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      me,
      isContext: this.options.app.state.get('isContext'),
      user: this.options.app.state.get('user')
    };
  }
});
