import _ from 'underscore';

const MAX_ITEMS = 10;
const STORAGE_KEY = 'sonar_recent_history';

class RecentHistory {
  static getRecentHistory () {
    let sonarHistory = localStorage.getItem(STORAGE_KEY);
    if (sonarHistory == null) {
      sonarHistory = [];
    } else {
      sonarHistory = JSON.parse(sonarHistory);
    }
    return sonarHistory;
  }

  static clear () {
    localStorage.removeItem(STORAGE_KEY);
  }

  static add (resourceKey, resourceName, icon) {
    if (resourceKey !== '') {
      let newEntry = { key: resourceKey, name: resourceName, icon: icon };

      let newHistory = this.getRecentHistory()
          .filter(item => item.key !== newEntry.key)
          .slice(0, MAX_ITEMS - 1);
      newHistory.unshift(newEntry);

      localStorage.setItem(STORAGE_KEY, JSON.stringify(newHistory));
    }
  }
}

export default RecentHistory;
