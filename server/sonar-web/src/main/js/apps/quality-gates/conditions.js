import Backbone from 'backbone';
import Condition from './condition';

export default Backbone.Collection.extend({
  model: Condition,
  comparator: 'metric'
});


