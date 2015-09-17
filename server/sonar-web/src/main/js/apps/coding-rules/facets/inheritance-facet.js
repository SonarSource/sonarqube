import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-inheritance-facet'],

  initialize: function (options) {
    this.listenTo(options.app.state, 'change:query', this.onQueryChange);
  },

  onQueryChange: function () {
    var query = this.options.app.state.get('query'),
        isProfileSelected = query.qprofile != null;
    if (isProfileSelected) {
      var profile = _.findWhere(this.options.app.qualityProfiles, { key: query.qprofile });
      if (profile != null && profile.parentKey == null) {
        this.forbid();
      }
    } else {
      this.forbid();
    }
  },

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    this.onQueryChange();
  },

  forbid: function () {
    BaseFacet.prototype.forbid.apply(this, arguments);
    this.$el.prop('title', t('coding_rules.filters.inheritance.inactive'));
  },

  allow: function () {
    BaseFacet.prototype.allow.apply(this, arguments);
    this.$el.prop('title', null);
  },

  getValues: function () {
    var values = ['NONE', 'INHERITED', 'OVERRIDES'];
    return values.map(function (key) {
      return {
        label: t('coding_rules.filters.inheritance', key.toLowerCase()),
        val: key
      };
    });
  },

  toggleFacet: function (e) {
    var obj = {},
        property = this.model.get('property');
    if ($(e.currentTarget).is('.active')) {
      obj[property] = null;
    } else {
      obj[property] = $(e.currentTarget).data('value');
    }
    this.options.app.state.updateFilter(obj);
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValues()
    });
  }
});


