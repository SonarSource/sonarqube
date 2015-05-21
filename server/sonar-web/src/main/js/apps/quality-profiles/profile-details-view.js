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
  './change-profile-parent-view',
  './profile-changelog-view',
  './profile-comparison-view',
  'components/common/select-list',
  './helpers',
  './templates'
], function (ChangeProfileParentView, ProfileChangelogView, ProfileComparisonView) {

  var $ = jQuery;

  return Marionette.Layout.extend({
    template: Templates['quality-profiles-profile-details'],

    regions: {
      changelogRegion: '#quality-profile-changelog',
      comparisonRegion: '#quality-profile-comparison'
    },

    modelEvents: {
      'change': 'render'
    },

    events: {
      'click .js-profile': 'onProfileClick',
      'click #quality-profile-change-parent': 'onChangeParentClick'
    },

    onRender: function () {
      if (!this.model.get('isDefault')) {
        this.initProjectsSelect();
      }
      this.changelogRegion.show(new ProfileChangelogView({ model: this.model }));
      this.comparisonRegion.show(new ProfileComparisonView({ model: this.model }));
      if (this.options.anchor === 'changelog') {
        this.scrollToChangelog();
      }
      if (this.options.anchor === 'comparison') {
        this.scrollToComparison();
      }
    },

    initProjectsSelect: function () {
      var key = this.model.get('key');
      this.projectsSelectList = new window.SelectList({
        el: this.$('#quality-profile-projects-list'),
        width: '100%',
        readOnly: !this.options.canWrite,
        focusSearch: false,
        format: function (item) {
          return item.name;
        },
        searchUrl: baseUrl + '/api/qualityprofiles/projects?key=' + encodeURIComponent(key),
        selectUrl: baseUrl + '/api/qualityprofiles/add_project',
        deselectUrl: baseUrl + '/api/qualityprofiles/remove_project',
        extra: {
          profileKey: key
        },
        selectParameter: 'projectUuid',
        selectParameterValue: 'uuid',
        labels: {
          selected: t('quality_gates.projects.with'),
          deselected: t('quality_gates.projects.without'),
          all: t('quality_gates.projects.all'),
          noResults: t('quality_gates.projects.noResults')
        },
        tooltips: {
          select: t('quality_gates.projects.select_hint'),
          deselect: t('quality_gates.projects.deselect_hint')
        }
      });
      this.listenTo(this.projectsSelectList.collection, 'change:selected', this.onProjectsChange);
    },

    onProfileClick: function (e) {
      var key = $(e.currentTarget).data('key'),
          profile = this.model.collection.get(key);
      if (profile != null) {
        e.preventDefault();
        this.model.collection.trigger('select', profile);
      }
    },

    onChangeParentClick: function (e) {
      e.preventDefault();
      this.changeParent();
    },

    onProjectsChange: function () {
      this.model.collection.updateForLanguage(this.model.get('language'));
    },

    changeParent: function () {
      new ChangeProfileParentView({
        model: this.model
      }).render();
    },

    scrollTo: function (selector) {
      var el = this.$(selector),
          parent = el.scrollParent();
      var elOffset = el.offset(),
          parentOffset = parent.offset();
      if (parent.is(document)) {
        parentOffset = { top: 0 };
      }
      if (elOffset != null && parentOffset != null) {
        var scrollTop = elOffset.top - parentOffset.top - 53;
        parent.scrollTop(scrollTop);
      }
    },

    scrollToChangelog: function () {
      this.scrollTo('#quality-profile-changelog');
    },

    scrollToComparison: function () {
      this.scrollTo('#quality-profile-comparison');
    },

    getExporters: function () {
      var language = this.model.get('language');
      return this.options.exporters.filter(function (exporter) {
        return exporter.languages.indexOf(language) !== -1;
      });
    },

    serializeData: function () {
      var key = this.model.get('key'),
          rulesSearchUrl = '/coding_rules#qprofile=' + encodeURIComponent(key) + '|activation=true';
      return _.extend(Marionette.ItemView.prototype.serializeData.apply(this, arguments), {
        rulesSearchUrl: rulesSearchUrl,
        canWrite: this.options.canWrite,
        exporters: this.getExporters()
      });
    }
  });

});
