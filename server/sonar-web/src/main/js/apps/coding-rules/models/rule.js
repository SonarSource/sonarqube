import _ from 'underscore';
import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  addExtraAttributes: function (repositories) {
    var repo = _.findWhere(repositories, { key: this.get('repo') }) || this.get('repo'),
        repoName = repo != null ? repo.name : repo,
        isManual = this.get('repo') === 'manual',
        isCustom = this.has('templateKey');
    this.set({
      repoName: repoName,
      isManual: isManual,
      isCustom: isCustom
    }, { silent: true });
  },

  getInactiveProfiles: function (actives, profiles) {
    return actives.map(function (profile) {
      var profileBase = _.findWhere(profiles, { key: profile.qProfile });
      if (profileBase != null) {
        _.extend(profile, profileBase);
      }
      return profile;
    });
  }
});


