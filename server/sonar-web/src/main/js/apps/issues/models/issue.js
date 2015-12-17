import Issue from '../../../components/issue/models/issue';

export default Issue.extend({
  reset: function (attrs, options) {
    var keepFields = ['index', 'selected', 'comments'];
    keepFields.forEach(function (field) {
      attrs[field] = this.get(field);
    }.bind(this));
    return Issue.prototype.reset.call(this, attrs, options);
  }
});


