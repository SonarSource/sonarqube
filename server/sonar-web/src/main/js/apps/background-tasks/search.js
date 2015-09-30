import $ from 'jquery';
import _ from 'underscore';
import React from 'react';
import RadioToggle from '../../components/shared/radio-toggle';
import {STATUSES, CURRENTS, DATE, DATE_FORMAT, DEBOUNCE_DELAY} from './constants';

export default React.createClass({
  componentDidUpdate() {
    this.attachDatePicker();
  },

  componentDidMount() {
    this.attachDatePicker();
  },

  componentWillMount() {
    this.onSearch = _.debounce(this.onSearch, DEBOUNCE_DELAY);
  },

  getCurrentsOptions() {
    return [
      { value: CURRENTS.ALL, label: 'All' },
      { value: CURRENTS.ONLY_CURRENTS, label: 'Only Currents' }
    ];
  },

  getStatusOptions() {
    return [
      { value: STATUSES.ALL, label: 'All' },
      { value: STATUSES.SUCCESS, label: 'Success' },
      { value: STATUSES.FAILED, label: 'Failed' },
      { value: STATUSES.CANCELED, label: 'Canceled' }
    ];
  },

  getDateOptions() {
    return [
      { value: DATE.ANY, label: 'Any Date' },
      { value: DATE.TODAY, label: 'Today' },
      { value: DATE.CUSTOM, label: 'Custom' }
    ];
  },

  onDateChange(newDate) {
    if (newDate === DATE.CUSTOM) {
      let minDateRaw = React.findDOMNode(this.refs.minDate).value,
          maxDateRaw = React.findDOMNode(this.refs.maxDate).value,
          minDate = moment(minDateRaw, DATE_FORMAT, true),
          maxDate = moment(maxDateRaw, DATE_FORMAT, true);
      this.props.onDateChange(newDate,
          minDate.isValid() ? minDate : null,
          maxDate.isValid() ? maxDate : null);
    } else {
      this.props.onDateChange(newDate);
    }
  },

  onDateInputChange() {
    this.onDateChange(DATE.CUSTOM);
  },

  attachDatePicker() {
    let opts = {
      dateFormat: 'yy-mm-dd',
      changeMonth: true,
      changeYear: true,
      onSelect: this.onDateInputChange
    };
    if ($.fn.datepicker) {
      $(React.findDOMNode(this.refs.minDate)).datepicker(opts);
      $(React.findDOMNode(this.refs.maxDate)).datepicker(opts);
    }
  },

  renderCustomDateInput() {
    let shouldBeVisible = this.props.dateFilter === DATE.CUSTOM,
        className = shouldBeVisible ? 'spacer-top' : 'spacer-top hidden';
    return (
        <div className={className}>
          from&nbsp;
          <input onChange={this.onDateInputChange} ref="minDate" type="text"/>
          &nbsp;to&nbsp;
          <input onChange={this.onDateInputChange} ref="maxDate" type="text"/>
        </div>
    );
  },

  onSearchFormSubmit(e) {
    e.preventDefault();
    this.onSearch();
  },

  onSearch() {
    let searchInput = React.findDOMNode(this.refs.searchInput),
        query = searchInput.value;
    this.props.onSearch(query);
  },

  renderSearchBox() {
    if (this.props.options.componentId) {
      // do not render search form on the project-level page
      return null;
    }
    return (
        <form onSubmit={this.onSearchFormSubmit} className="search-box">
          <button className="search-box-submit button-clean">
            <i className="icon-search"></i>
          </button>
          <input onChange={this.onSearch} ref="searchInput" className="search-box-input" type="search"
                 placeholder="Search"/>
        </form>
    );
  },

  render() {
    return (
        <section className="big-spacer-top big-spacer-bottom">
          <ul className="list-inline">
            <li>
              <RadioToggle options={this.getStatusOptions()} value={this.props.statusFilter}
                           name="background-task-status" onCheck={this.props.onStatusChange}/>
            </li>
            <li>
              <RadioToggle options={this.getCurrentsOptions()} value={this.props.currentsFilter}
                           name="background-task-currents" onCheck={this.props.onCurrentsChange}/>
            </li>
            <li>
              <RadioToggle options={this.getDateOptions()} value={this.props.dateFilter}
                           name="background-task-date" onCheck={this.onDateChange}/>
              {this.renderCustomDateInput()}
            </li>
            <li>{this.renderSearchBox()}</li>
          </ul>
        </section>
    );
  }
});
