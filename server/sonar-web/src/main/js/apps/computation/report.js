import Backbone from 'backbone';

export default Backbone.Model.extend({
  idAttribute: 'key',

  getDuration: function () {
    var duration = null;
    if (this.has('startedAt')) {
      var startedAtMoment = moment(this.get('startedAt')),
          finishedAtMoment = moment(this.get('finishedAt') || new Date()),
          diff = finishedAtMoment.diff(startedAtMoment);
      duration = {
        seconds: Math.floor(diff / 1000) % 60,
        minutes: Math.floor(diff / (1000 * 60)) % 60,
        hours: Math.floor(diff / (1000 * 60 * 60)) % 24
      };
    }
    return duration;
  },

  isDanger: function () {
    var dangerStatuses = ['CANCELLED', 'FAILED'];
    return dangerStatuses.indexOf(this.get('status')) !== -1;
  }
});


