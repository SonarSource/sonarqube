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
// @flow
import $ from 'jquery';
import Item from './models/item';
import Items from './models/items';
import ItemsView from './views/items-view';
import ViewerView from './views/viewer-view';
import RuleView from './views/rule-view';
import { getRuleDetails } from '../../api/rules';
import './styles.css';
import '../../apps/coding-rules/styles.css';

let instance = null;
const Workspace = function() {
  if (instance != null) {
    throw new Error('Cannot instantiate more than one Workspace, use Workspace.getInstance()');
  }
  this.initialize();
};

Workspace.prototype = {
  initialize() {
    const that = this;

    this.items = new Items();
    this.items.load();
    this.items.on('change', () => {
      that.save();
    });

    this.itemsView = new ItemsView({ collection: this.items });
    this.itemsView.render().$el.appendTo(document.body);
    this.itemsView.on('click', model => {
      that.open(model);
    });
  },

  save() {
    this.items.save();
  },

  addComponent(model) {
    const m = this.items.add2(model);
    this.save();
    return m;
  },

  open(options) {
    const model = typeof options.toJSON === 'function' ? options : new Item(options);
    if (!model.isValid()) {
      throw new Error(model.validationError);
    }
    const m = this.addComponent(model);
    if (m.isComponent()) {
      this.showComponentViewer(m);
    }
    if (m.isRule()) {
      this.showRule(m);
    }
  },

  openComponent(options) {
    return this.open({ ...options, __type__: 'component' });
  },

  openRule(options /*: { key: string, organization: string } */) {
    return this.open({ ...options, __type__: 'rule' });
  },

  showViewer(Viewer, model) {
    const that = this;
    if (this.viewerView != null) {
      this.viewerView.model.trigger('hideViewer');
      this.viewerView.destroy();
    }
    $('html').addClass('with-workspace');
    model.trigger('showViewer');
    this.viewerView = new Viewer({ model });
    this.viewerView
      .on('viewerMinimize', () => {
        model.trigger('hideViewer');
        that.closeComponentViewer();
      })
      .on('viewerClose', m => {
        that.closeComponentViewer();
        m.destroy();
      });
    this.viewerView.$el.appendTo(document.body);
    this.viewerView.render();
  },

  showComponentViewer(model) {
    this.showViewer(ViewerView, model);
  },

  closeComponentViewer() {
    if (this.viewerView != null) {
      this.viewerView.destroy();
      $('.with-workspace').removeClass('with-workspace');
    }
  },

  showRule(model) {
    const that = this;
    getRuleDetails({ key: model.get('key') }).then(
      r => {
        model.set({ ...r.rule, exist: true });
        that.showViewer(RuleView, model);
      },
      () => {
        model.set({ exist: false });
        that.showViewer(RuleView, model);
      }
    );
  }
};

Workspace.getInstance = function() {
  if (instance == null) {
    instance = new Workspace();
  }
  return instance;
};

export default Workspace.getInstance();
