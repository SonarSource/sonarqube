import $ from 'jquery';
import BaseFacet from './base-facet';
import '../templates';

export default BaseFacet.extend({
  template: Templates['coding-rules-template-facet'],

  onRender: function () {
    BaseFacet.prototype.onRender.apply(this, arguments);
    var value = this.options.app.state.get('query').is_template;
    if (value != null) {
      this.$('.js-facet').filter('[data-value="' + value + '"]').addClass('active');
    }
  },

  toggleFacet: function (e) {
    $(e.currentTarget).toggleClass('active');
    var property = this.model.get('property'),
        obj = {};
    if ($(e.currentTarget).hasClass('active')) {
      obj[property] = '' + $(e.currentTarget).data('value');
    } else {
      obj[property] = null;
    }
    this.options.app.state.updateFilter(obj);
  }

});


