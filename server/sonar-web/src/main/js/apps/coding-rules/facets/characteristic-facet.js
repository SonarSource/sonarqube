import $ from 'jquery';
import _ from 'underscore';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-characteristic-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').has_debt_characteristic;
    if (value != null && ('' + value === 'false')) {
      this.$('.js-facet').filter('[data-empty-characteristic]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    var noneCharacteristic = $(e.currentTarget).is('[data-empty-characteristic]'),
        property = this.model.get('property'),
        obj = {};
    $(e.currentTarget).toggleClass('active');
    if (noneCharacteristic) {
      var checked = $(e.currentTarget).is('.active');
      obj.has_debt_characteristic = checked ? 'false' : null;
      obj[property] = null;
    } else {
      obj.has_debt_characteristic = null;
      obj[property] = this.getValue();
    }
    this.options.app.state.updateFilter(obj);
  },

  disable: function () {
    var property = this.model.get('property'),
        obj = {};
    obj.has_debt_characteristic = null;
    obj[property] = null;
    this.options.app.state.updateFilter(obj);
  },

  getValues: function () {
    var values = this.model.getValues(),
        characteristics = this.options.app.characteristics;
    return values.map(function (value) {
      var ch = _.findWhere(characteristics, { key: value.val });
      if (ch != null) {
        _.extend(value, ch, { label: ch.name });
      }
      return value;
    });
  },

  sortValues: function (values) {
    return _.sortBy(values, 'index');
  },

  serializeData: function () {
    return _.extend(BaseFacet.prototype.serializeData.apply(this, arguments), {
      values: this.sortValues(this.getValues())
    });
  }
});


