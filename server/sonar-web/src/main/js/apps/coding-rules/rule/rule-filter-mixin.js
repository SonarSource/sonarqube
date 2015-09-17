import $ from 'jquery';
import RuleFilterView from '../rule-filter-view';

export default {
  onRuleFilterClick: function (e) {
    e.preventDefault();
    e.stopPropagation();
    $('body').click();
    var that = this,
        popup = new RuleFilterView({
          triggerEl: $(e.currentTarget),
          bottomRight: true,
          model: this.model
        });
    popup.on('select', function (property, value) {
      var obj = {};
      obj[property] = '' + value;
      that.options.app.state.updateFilter(obj);
      popup.destroy();
    });
    popup.render();
  }
};


