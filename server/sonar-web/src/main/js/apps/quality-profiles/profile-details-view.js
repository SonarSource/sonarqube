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
import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import escapeHtml from 'escape-html';
import ChangeProfileParentView from './change-profile-parent-view';
import ProfileChangelogView from './profile-changelog-view';
import ProfileComparisonView from './profile-comparison-view';
import '../../components/SelectList';
import Template from './templates/quality-profiles-profile-details.hbs';
import { translate } from '../../helpers/l10n';

export default Marionette.LayoutView.extend({
  template: Template,

  regions: {
    changelogRegion: '#quality-profile-changelog',
    comparisonRegion: '#quality-profile-comparison'
  },

  modelEvents: {
    'change': 'onChange',
    'flashChangelog': 'flashChangelog'
  },

  events: {
    'click .js-profile': 'onProfileClick',
    'click #quality-profile-change-parent': 'onChangeParentClick'
  },

  onRender () {
    if (!this.model.get('isDefault')) {
      this.initProjectsSelect();
    }
    this.changelogRegion.show(new ProfileChangelogView({ model: this.model }));
    this.comparisonRegion.show(new ProfileComparisonView({ model: this.model }));
    if (this.options.anchor === 'changelog') {
      this.scrollToChangelog();
      this.flashChangelog();
    }
    if (this.options.anchor === 'comparison') {
      this.scrollToComparison();
    }
    this.$('#quality-profile-changelog-form input')
        .datepicker({
          dateFormat: 'yy-mm-dd',
          changeMonth: true,
          changeYear: true
        });
  },

  onChange () {
    const changed = Object.keys(this.model.changedAttributes());
    if (!(changed.length === 1 && changed[0] === 'projectCount')) {
      this.render();
    }
  },

  initProjectsSelect () {
    const key = this.model.get('key');
    this.projectsSelectList = new window.SelectList({
      el: this.$('#quality-profile-projects-list'),
      width: '100%',
      height: 200,
      readOnly: !this.options.canWrite,
      focusSearch: false,
      dangerouslyUnescapedHtmlFormat: item => escapeHtml(item.name),
      searchUrl: window.baseUrl +
        '/api/qualityprofiles/projects?key=' +
        encodeURIComponent(key),
      selectUrl: window.baseUrl + '/api/qualityprofiles/add_project',
      deselectUrl: window.baseUrl + '/api/qualityprofiles/remove_project',
      extra: {
        profileKey: key
      },
      selectParameter: 'projectUuid',
      selectParameterValue: 'uuid',
      labels: {
        selected: translate('quality_gates.projects.with'),
        deselected: translate('quality_gates.projects.without'),
        all: translate('quality_gates.projects.all'),
        noResults: translate('quality_gates.projects.noResults')
      },
      tooltips: {
        select: translate('quality_profiles.projects.select_hint'),
        deselect: translate('quality_profiles.projects.deselect_hint')
      }
    });
    this.listenTo(this.projectsSelectList.collection, 'change:selected', this.onProjectsChange);
  },

  onProfileClick (e) {
    const key = $(e.currentTarget).data('key');
    const profile = this.model.collection.get(key);
    if (profile != null) {
      e.preventDefault();
      this.model.collection.trigger('select', profile);
    }
  },

  onChangeParentClick (e) {
    e.preventDefault();
    this.changeParent();
  },

  onProjectsChange () {
    this.model.collection.updateForLanguage(this.model.get('language'));
  },

  changeParent () {
    new ChangeProfileParentView({
      model: this.model
    }).render();
  },

  scrollTo (selector) {
    const el = this.$(selector);
    const parent = el.scrollParent();
    const elOffset = el.offset();
    let parentOffset = parent.offset();
    if (parent.is(document)) {
      parentOffset = { top: 0 };
    }
    if (elOffset != null && parentOffset != null) {
      const scrollTop = elOffset.top - parentOffset.top - 53;
      parent.scrollTop(scrollTop);
    }
  },

  scrollToChangelog () {
    this.scrollTo('#quality-profile-changelog');
  },

  scrollToComparison () {
    this.scrollTo('#quality-profile-comparison');
  },

  getExporters () {
    const language = this.model.get('language');
    return this.options.exporters.filter(function (exporter) {
      return exporter.languages.indexOf(language) !== -1;
    });
  },

  flashChangelog () {
    const changelogEl = this.$(this.changelogRegion.el);
    changelogEl.addClass('flash in');
    setTimeout(function () {
      changelogEl.removeClass('in');
    }, 2000);
  },

  serializeData () {
    const key = this.model.get('key');
    const rulesSearchUrl = `/coding_rules#qprofile=${encodeURIComponent(key)}|activation=true`;
    const activateRulesUrl = `/coding_rules#qprofile=${encodeURIComponent(key)}|activation=false`;
    return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
      rulesSearchUrl,
      activateRulesUrl,
      canWrite: this.options.canWrite,
      exporters: this.getExporters()
    });
  }
});
