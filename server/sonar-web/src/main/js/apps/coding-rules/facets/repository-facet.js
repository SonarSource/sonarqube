import _ from 'underscore';
import CustomValuesFacet from './custom-values-facet';

export default CustomValuesFacet.extend({

  getUrl: function () {
    return baseUrl + '/api/rules/repositories';
  },

  prepareAjaxSearch: function () {
    return {
      quietMillis: 300,
      url: this.getUrl(),
      data: function (term) {
        return { q: term, ps: 10000 };
      },
      results: function (data) {
        return {
          more: false,
          results: data.repositories.map(function (repo) {
            return { id: repo.key, text: repo.name + ' (' + repo.language + ')' };
          })
        };
      }
    };
  },

  getLabelsSource: function () {
    var repos = this.options.app.repositories;
    return _.object(_.pluck(repos, 'key'), _.pluck(repos, 'name'));
  },

  getValues: function () {
    var that = this,
        labels = that.getLabelsSource();
    return this.model.getValues().map(function (value) {
      var repo = _.findWhere(that.options.app.repositories, { key: value.val });
      if (repo != null) {
        var langName = that.options.app.languages[repo.language];
        _.extend(value, { extra: langName });
      }
      return _.extend(value, { label: labels[value.val] });
    });
  },

  serializeData: function () {
    return _.extend(CustomValuesFacet.prototype.serializeData.apply(this, arguments), {
      values: this.getValues()
    });
  }

});


