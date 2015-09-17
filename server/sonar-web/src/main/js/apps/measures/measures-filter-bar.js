import $ from 'jquery';
import FilterBarView from 'components/navigator/filters/filter-bar';

export default FilterBarView.extend({
  template: function () {
    return $('#filter-bar-template').html();
  }
});


