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
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import DetailsView from './details-view';
import HeaderView from './header-view';

export default Marionette.Controller.extend({

  initialize (options) {
    this.app = options.app;
    this.canEdit = this.app.canEdit;
    this.listenTo(this.app.gates, 'select', this.onSelect);
    this.listenTo(this.app.gates, 'destroy', this.onDestroy);
  },

  index () {
    this.app.gates.fetch();
  },

  show (id) {
    const that = this;
    this.app.gates.fetch().done(function () {
      const gate = that.app.gates.get(id);
      if (gate != null) {
        gate.trigger('select', gate, { trigger: false });
      }
    });
  },

  onSelect (gate, options) {
    const that = this;
    const route = 'show/' + gate.id;
    const opts = _.defaults(options || {}, { trigger: true });
    if (opts.trigger) {
      this.app.router.navigate(route);
    }
    this.app.gatesView.highlight(gate.id);
    gate.fetch().done(function () {
      const headerView = new HeaderView({
        model: gate,
        canEdit: that.canEdit
      });
      that.app.layout.headerRegion.show(headerView);

      const detailsView = new DetailsView({
        model: gate,
        canEdit: that.canEdit,
        metrics: that.app.metrics,
        periods: that.app.periods
      });
      that.app.layout.detailsRegion.show(detailsView);
    });
  },

  onDestroy () {
    this.app.router.navigate('');
    this.app.layout.headerRegion.reset();
    this.app.layout.detailsRegion.reset();
    this.app.layout.renderIntro();
    this.app.gatesView.highlight(null);
  }

});


