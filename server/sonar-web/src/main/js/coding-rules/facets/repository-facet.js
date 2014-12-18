define([
  'coding-rules/facets/custom-labels-facet'
], function (CustomLabelsFacet) {

  return CustomLabelsFacet.extend({

    getLabelsSource: function () {
      var repos = this.options.app.repositories;
      return _.object(_.pluck(repos, 'key'), _.pluck(repos, 'name'));
    },

    getValues: function () {
      var that = this,
          values = CustomLabelsFacet.prototype.getValues.apply(this, arguments);
      return values.map(function (value) {
        var repo = _.findWhere(that.options.app.repositories, { key: value.val });
        if (repo != null) {
          var langName = that.options.app.languages[repo.language];
          _.extend(value, { extra: langName });
        }
        return value;
      });
    }

  });

});
