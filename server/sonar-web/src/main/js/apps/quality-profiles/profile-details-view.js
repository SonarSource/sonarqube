import $ from 'jquery';
import _ from 'underscore';
import Marionette from 'backbone.marionette';
import ChangeProfileParentView from './change-profile-parent-view';
import ProfileChangelogView from './profile-changelog-view';
import ProfileComparisonView from './profile-comparison-view';
import 'components/common/select-list';
import './helpers';
import './templates';

export default Marionette.LayoutView.extend({
  template: Templates['quality-profiles-profile-details'],

  regions: {
    changelogRegion: '#quality-profile-changelog',
    comparisonRegion: '#quality-profile-comparison'
  },

  modelEvents: {
    'change': 'render',
    'flashChangelog': 'flashChangelog'
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

  initProjectsSelect: function () {
    var key = this.model.get('key');
    this.projectsSelectList = new window.SelectList({
      el: this.$('#quality-profile-projects-list'),
      width: '100%',
      height: 200,
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

  flashChangelog: function () {
    var changelogEl = this.$(this.changelogRegion.el);
    changelogEl.addClass('flash in');
    setTimeout(function () {
      changelogEl.removeClass('in');
    }, 2000);
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


