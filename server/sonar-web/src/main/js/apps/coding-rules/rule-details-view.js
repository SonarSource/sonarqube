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
import Backbone from 'backbone';
import Marionette from 'backbone.marionette';
import key from 'keymaster';
import Rules from './models/rules';
import MetaView from './rule/rule-meta-view';
import DescView from './rule/rule-description-view';
import ParamView from './rule/rule-parameters-view';
import ProfilesView from './rule/rule-profiles-view';
import CustomRulesView from './rule/custom-rules-view';
import CustomRuleCreationView from './rule/custom-rule-creation-view';
import DeleteRuleView from './rule/delete-rule-view';
import IssuesView from './rule/rule-issues-view';
import Template from './templates/coding-rules-rule-details.hbs';
import { searchRules } from '../../api/rules';

export default Marionette.LayoutView.extend({
  className: 'coding-rule-details',
  template: Template,

  regions: {
    metaRegion: '.js-rule-meta',
    descRegion: '.js-rule-description',
    paramRegion: '.js-rule-parameters',
    profilesRegion: '.js-rule-profiles',
    customRulesRegion: '.js-rule-custom-rules',
    issuesRegion: '.js-rule-issues'
  },

  events: {
    'click .js-edit-custom': 'editCustomRule',
    'click .js-delete': 'deleteRule'
  },

  initialize() {
    this.bindShortcuts();
    this.customRules = new Rules();
    if (this.model.get('isTemplate')) {
      this.fetchCustomRules();
    }
    this.listenTo(this.options.app.state, 'change:selectedIndex', this.select);
  },

  onRender() {
    this.metaRegion.show(
      new MetaView({
        app: this.options.app,
        model: this.model
      })
    );
    this.descRegion.show(
      new DescView({
        app: this.options.app,
        model: this.model
      })
    );
    this.paramRegion.show(
      new ParamView({
        app: this.options.app,
        model: this.model
      })
    );
    this.profilesRegion.show(
      new ProfilesView({
        app: this.options.app,
        model: this.model,
        collection: new Backbone.Collection(this.getQualityProfiles())
      })
    );
    this.customRulesRegion.show(
      new CustomRulesView({
        app: this.options.app,
        model: this.model,
        collection: this.customRules
      })
    );
    this.issuesRegion.show(
      new IssuesView({
        app: this.options.app,
        model: this.model
      })
    );
    this.$el.scrollParent().scrollTop(0);
  },

  onDestroy() {
    this.unbindShortcuts();
  },

  fetchCustomRules() {
    const options = {
      template_key: this.model.get('key'),
      f: 'name,severity,params'
    };
    searchRules(options).then(r => this.customRules.reset(r.rules), () => {});
  },

  getQualityProfiles() {
    return this.model.getInactiveProfiles(this.options.actives, this.options.app.qualityProfiles);
  },

  bindShortcuts() {
    const that = this;
    key('up', 'details', () => {
      that.options.app.controller.selectPrev();
      return false;
    });
    key('down', 'details', () => {
      that.options.app.controller.selectNext();
      return false;
    });
    key('left, backspace', 'details', () => {
      that.options.app.controller.hideDetails();
      return false;
    });
  },

  unbindShortcuts() {
    key.deleteScope('details');
  },

  editCustomRule() {
    new CustomRuleCreationView({
      app: this.options.app,
      model: this.model
    }).render();
  },

  deleteRule() {
    const deleteRuleView = new DeleteRuleView({
      model: this.model
    }).render();

    deleteRuleView.on('delete', () => {
      const { controller } = this.options.app;
      if (controller.isRulePermalink()) {
        controller.newSearch();
      } else {
        controller.fetchList();
      }
    });
  },

  select() {
    const selected = this.options.app.state.get('selectedIndex');
    const selectedRule = this.options.app.list.at(selected);
    this.options.app.controller.showDetails(selectedRule);
  },

  serializeData() {
    const isCustom = this.model.has('templateKey');
    const isEditable = this.options.app.canWrite && this.options.app.customRules && isCustom;
    let qualityProfilesVisible = true;

    if (this.model.get('isTemplate')) {
      qualityProfilesVisible = Object.keys(this.options.actives).length > 0;
    }

    return {
      ...Marionette.ItemView.prototype.serializeData.apply(this, arguments),
      isEditable,
      qualityProfilesVisible,
      allTags: union(this.model.get('sysTags'), this.model.get('tags'))
    };
  }
});
