/*
Copyright (c) 2007, Yahoo! Inc. All rights reserved.
Code licensed under the BSD License:
http://developer.yahoo.net/yui/license.txt
version: 2.4.1
*/
(function () {

    /**
    * Config is a utility used within an Object to allow the implementer to
    * maintain a list of local configuration properties and listen for changes 
    * to those properties dynamically using CustomEvent. The initial values are 
    * also maintained so that the configuration can be reset at any given point 
    * to its initial state.
    * @namespace YAHOO.util
    * @class Config
    * @constructor
    * @param {Object} owner The owner Object to which this Config Object belongs
    */
    YAHOO.util.Config = function (owner) {

        if (owner) {
            this.init(owner);
        }


    };


    var Lang = YAHOO.lang,
        CustomEvent = YAHOO.util.CustomEvent,
        Config = YAHOO.util.Config;


    /**
     * Constant representing the CustomEvent type for the config changed event.
     * @property YAHOO.util.Config.CONFIG_CHANGED_EVENT
     * @private
     * @static
     * @final
     */
    Config.CONFIG_CHANGED_EVENT = "configChanged";
    
    /**
     * Constant representing the boolean type string
     * @property YAHOO.util.Config.BOOLEAN_TYPE
     * @private
     * @static
     * @final
     */
    Config.BOOLEAN_TYPE = "boolean";
    
    Config.prototype = {
     
        /**
        * Object reference to the owner of this Config Object
        * @property owner
        * @type Object
        */
        owner: null,
        
        /**
        * Boolean flag that specifies whether a queue is currently 
        * being executed
        * @property queueInProgress
        * @type Boolean
        */
        queueInProgress: false,
        
        /**
        * Maintains the local collection of configuration property objects and 
        * their specified values
        * @property config
        * @private
        * @type Object
        */ 
        config: null,
        
        /**
        * Maintains the local collection of configuration property objects as 
        * they were initially applied.
        * This object is used when resetting a property.
        * @property initialConfig
        * @private
        * @type Object
        */ 
        initialConfig: null,
        
        /**
        * Maintains the local, normalized CustomEvent queue
        * @property eventQueue
        * @private
        * @type Object
        */ 
        eventQueue: null,
        
        /**
        * Custom Event, notifying subscribers when Config properties are set 
        * (setProperty is called without the silent flag
        * @event configChangedEvent
        */
        configChangedEvent: null,
    
        /**
        * Initializes the configuration Object and all of its local members.
        * @method init
        * @param {Object} owner The owner Object to which this Config 
        * Object belongs
        */
        init: function (owner) {
    
            this.owner = owner;
    
            this.configChangedEvent = 
                this.createEvent(Config.CONFIG_CHANGED_EVENT);
    
            this.configChangedEvent.signature = CustomEvent.LIST;
            this.queueInProgress = false;
            this.config = {};
            this.initialConfig = {};
            this.eventQueue = [];
        
        },
        
        /**
        * Validates that the value passed in is a Boolean.
        * @method checkBoolean
        * @param {Object} val The value to validate
        * @return {Boolean} true, if the value is valid
        */ 
        checkBoolean: function (val) {
            return (typeof val == Config.BOOLEAN_TYPE);
        },
        
        /**
        * Validates that the value passed in is a number.
        * @method checkNumber
        * @param {Object} val The value to validate
        * @return {Boolean} true, if the value is valid
        */
        checkNumber: function (val) {
            return (!isNaN(val));
        },
        
        /**
        * Fires a configuration property event using the specified value. 
        * @method fireEvent
        * @private
        * @param {String} key The configuration property's name
        * @param {value} Object The value of the correct type for the property
        */ 
        fireEvent: function ( key, value ) {
            var property = this.config[key];
        
            if (property && property.event) {
                property.event.fire(value);
            } 
        },
        
        /**
        * Adds a property to the Config Object's private config hash.
        * @method addProperty
        * @param {String} key The configuration property's name
        * @param {Object} propertyObject The Object containing all of this 
        * property's arguments
        */
        addProperty: function ( key, propertyObject ) {
            key = key.toLowerCase();
        
            this.config[key] = propertyObject;
        
            propertyObject.event = this.createEvent(key, { scope: this.owner });
            propertyObject.event.signature = CustomEvent.LIST;
            
            
            propertyObject.key = key;
        
            if (propertyObject.handler) {
                propertyObject.event.subscribe(propertyObject.handler, 
                    this.owner);
            }
        
            this.setProperty(key, propertyObject.value, true);
            
            if (! propertyObject.suppressEvent) {
                this.queueProperty(key, propertyObject.value);
            }
            
        },
        
        /**
        * Returns a key-value configuration map of the values currently set in  
        * the Config Object.
        * @method getConfig
        * @return {Object} The current config, represented in a key-value map
        */
        getConfig: function () {
        
            var cfg = {},
                prop,
                property;
                
            for (prop in this.config) {
                property = this.config[prop];
                if (property && property.event) {
                    cfg[prop] = property.value;
                }
            }
            
            return cfg;
        },
        
        /**
        * Returns the value of specified property.
        * @method getProperty
        * @param {String} key The name of the property
        * @return {Object}  The value of the specified property
        */
        getProperty: function (key) {
            var property = this.config[key.toLowerCase()];
            if (property && property.event) {
                return property.value;
            } else {
                return undefined;
            }
        },
        
        /**
        * Resets the specified property's value to its initial value.
        * @method resetProperty
        * @param {String} key The name of the property
        * @return {Boolean} True is the property was reset, false if not
        */
        resetProperty: function (key) {
    
            key = key.toLowerCase();
        
            var property = this.config[key];
    
            if (property && property.event) {
    
                if (this.initialConfig[key] && 
                    !Lang.isUndefined(this.initialConfig[key])) {
    
                    this.setProperty(key, this.initialConfig[key]);

                    return true;
    
                }
    
            } else {
    
                return false;
            }
    
        },
        
        /**
        * Sets the value of a property. If the silent property is passed as 
        * true, the property's event will not be fired.
        * @method setProperty
        * @param {String} key The name of the property
        * @param {String} value The value to set the property to
        * @param {Boolean} silent Whether the value should be set silently, 
        * without firing the property event.
        * @return {Boolean} True, if the set was successful, false if it failed.
        */
        setProperty: function (key, value, silent) {
        
            var property;
        
            key = key.toLowerCase();
        
            if (this.queueInProgress && ! silent) {
                // Currently running through a queue... 
                this.queueProperty(key,value);
                return true;
    
            } else {
                property = this.config[key];
                if (property && property.event) {
                    if (property.validator && !property.validator(value)) {
                        return false;
                    } else {
                        property.value = value;
                        if (! silent) {
                            this.fireEvent(key, value);
                            this.configChangedEvent.fire([key, value]);
                        }
                        return true;
                    }
                } else {
                    return false;
                }
            }
        },
        
        /**
        * Sets the value of a property and queues its event to execute. If the 
        * event is already scheduled to execute, it is
        * moved from its current position to the end of the queue.
        * @method queueProperty
        * @param {String} key The name of the property
        * @param {String} value The value to set the property to
        * @return {Boolean}  true, if the set was successful, false if 
        * it failed.
        */ 
        queueProperty: function (key, value) {
        
            key = key.toLowerCase();
        
            var property = this.config[key],
                foundDuplicate = false,
                iLen,
                queueItem,
                queueItemKey,
                queueItemValue,
                sLen,
                supercedesCheck,
                qLen,
                queueItemCheck,
                queueItemCheckKey,
                queueItemCheckValue,
                i,
                s,
                q;
                                
            if (property && property.event) {
    
                if (!Lang.isUndefined(value) && property.validator && 
                    !property.validator(value)) { // validator
                    return false;
                } else {
        
                    if (!Lang.isUndefined(value)) {
                        property.value = value;
                    } else {
                        value = property.value;
                    }
        
                    foundDuplicate = false;
                    iLen = this.eventQueue.length;
        
                    for (i = 0; i < iLen; i++) {
                        queueItem = this.eventQueue[i];
        
                        if (queueItem) {
                            queueItemKey = queueItem[0];
                            queueItemValue = queueItem[1];

                            if (queueItemKey == key) {
    
                                /*
                                    found a dupe... push to end of queue, null 
                                    current item, and break
                                */
    
                                this.eventQueue[i] = null;
    
                                this.eventQueue.push(
                                    [key, (!Lang.isUndefined(value) ? 
                                    value : queueItemValue)]);
    
                                foundDuplicate = true;
                                break;
                            }
                        }
                    }
                    
                    // this is a refire, or a new property in the queue
    
                    if (! foundDuplicate && !Lang.isUndefined(value)) { 
                        this.eventQueue.push([key, value]);
                    }
                }
        
                if (property.supercedes) {

                    sLen = property.supercedes.length;

                    for (s = 0; s < sLen; s++) {

                        supercedesCheck = property.supercedes[s];
                        qLen = this.eventQueue.length;

                        for (q = 0; q < qLen; q++) {
                            queueItemCheck = this.eventQueue[q];

                            if (queueItemCheck) {
                                queueItemCheckKey = queueItemCheck[0];
                                queueItemCheckValue = queueItemCheck[1];

                                if (queueItemCheckKey == 
                                    supercedesCheck.toLowerCase() ) {

                                    this.eventQueue.push([queueItemCheckKey, 
                                        queueItemCheckValue]);

                                    this.eventQueue[q] = null;
                                    break;

                                }
                            }
                        }
                    }
                }


                return true;
            } else {
                return false;
            }
        },
        
        /**
        * Fires the event for a property using the property's current value.
        * @method refireEvent
        * @param {String} key The name of the property
        */
        refireEvent: function (key) {
    
            key = key.toLowerCase();
        
            var property = this.config[key];
    
            if (property && property.event && 
    
                !Lang.isUndefined(property.value)) {
    
                if (this.queueInProgress) {
    
                    this.queueProperty(key);
    
                } else {
    
                    this.fireEvent(key, property.value);
    
                }
    
            }
        },
        
        /**
        * Applies a key-value Object literal to the configuration, replacing  
        * any existing values, and queueing the property events.
        * Although the values will be set, fireQueue() must be called for their 
        * associated events to execute.
        * @method applyConfig
        * @param {Object} userConfig The configuration Object literal
        * @param {Boolean} init  When set to true, the initialConfig will 
        * be set to the userConfig passed in, so that calling a reset will 
        * reset the properties to the passed values.
        */
        applyConfig: function (userConfig, init) {
        
            var sKey,
                oConfig;

            if (init) {
                oConfig = {};
                for (sKey in userConfig) {
                    if (Lang.hasOwnProperty(userConfig, sKey)) {
                        oConfig[sKey.toLowerCase()] = userConfig[sKey];
                    }
                }
                this.initialConfig = oConfig;
            }

            for (sKey in userConfig) {
                if (Lang.hasOwnProperty(userConfig, sKey)) {
                    this.queueProperty(sKey, userConfig[sKey]);
                }
            }
        },
        
        /**
        * Refires the events for all configuration properties using their 
        * current values.
        * @method refresh
        */
        refresh: function () {
        
            var prop;
        
            for (prop in this.config) {
                this.refireEvent(prop);
            }
        },
        
        /**
        * Fires the normalized list of queued property change events
        * @method fireQueue
        */
        fireQueue: function () {
        
            var i, 
                queueItem,
                key,
                value,
                property;
        
            this.queueInProgress = true;
            for (i = 0;i < this.eventQueue.length; i++) {
                queueItem = this.eventQueue[i];
                if (queueItem) {
        
                    key = queueItem[0];
                    value = queueItem[1];
                    property = this.config[key];
        
                    property.value = value;
        
                    this.fireEvent(key,value);
                }
            }
            
            this.queueInProgress = false;
            this.eventQueue = [];
        },
        
        /**
        * Subscribes an external handler to the change event for any 
        * given property. 
        * @method subscribeToConfigEvent
        * @param {String} key The property name
        * @param {Function} handler The handler function to use subscribe to 
        * the property's event
        * @param {Object} obj The Object to use for scoping the event handler 
        * (see CustomEvent documentation)
        * @param {Boolean} override Optional. If true, will override "this"  
        * within the handler to map to the scope Object passed into the method.
        * @return {Boolean} True, if the subscription was successful, 
        * otherwise false.
        */ 
        subscribeToConfigEvent: function (key, handler, obj, override) {
    
            var property = this.config[key.toLowerCase()];
    
            if (property && property.event) {
                if (!Config.alreadySubscribed(property.event, handler, obj)) {
                    property.event.subscribe(handler, obj, override);
                }
                return true;
            } else {
                return false;
            }
    
        },
        
        /**
        * Unsubscribes an external handler from the change event for any 
        * given property. 
        * @method unsubscribeFromConfigEvent
        * @param {String} key The property name
        * @param {Function} handler The handler function to use subscribe to 
        * the property's event
        * @param {Object} obj The Object to use for scoping the event 
        * handler (see CustomEvent documentation)
        * @return {Boolean} True, if the unsubscription was successful, 
        * otherwise false.
        */
        unsubscribeFromConfigEvent: function (key, handler, obj) {
            var property = this.config[key.toLowerCase()];
            if (property && property.event) {
                return property.event.unsubscribe(handler, obj);
            } else {
                return false;
            }
        },
        
        /**
        * Returns a string representation of the Config object
        * @method toString
        * @return {String} The Config object in string format.
        */
        toString: function () {
            var output = "Config";
            if (this.owner) {
                output += " [" + this.owner.toString() + "]";
            }
            return output;
        },
        
        /**
        * Returns a string representation of the Config object's current 
        * CustomEvent queue
        * @method outputEventQueue
        * @return {String} The string list of CustomEvents currently queued 
        * for execution
        */
        outputEventQueue: function () {

            var output = "",
                queueItem,
                q,
                nQueue = this.eventQueue.length;
              
            for (q = 0; q < nQueue; q++) {
                queueItem = this.eventQueue[q];
                if (queueItem) {
                    output += queueItem[0] + "=" + queueItem[1] + ", ";
                }
            }
            return output;
        },

        /**
        * Sets all properties to null, unsubscribes all listeners from each 
        * property's change event and all listeners from the configChangedEvent.
        * @method destroy
        */
        destroy: function () {

            var oConfig = this.config,
                sProperty,
                oProperty;


            for (sProperty in oConfig) {
            
                if (Lang.hasOwnProperty(oConfig, sProperty)) {

                    oProperty = oConfig[sProperty];

                    oProperty.event.unsubscribeAll();
                    oProperty.event = null;

                }
            
            }
            
            this.configChangedEvent.unsubscribeAll();
            
            this.configChangedEvent = null;
            this.owner = null;
            this.config = null;
            this.initialConfig = null;
            this.eventQueue = null;
        
        }

    };
    
    
    
    /**
    * Checks to determine if a particular function/Object pair are already 
    * subscribed to the specified CustomEvent
    * @method YAHOO.util.Config.alreadySubscribed
    * @static
    * @param {YAHOO.util.CustomEvent} evt The CustomEvent for which to check 
    * the subscriptions
    * @param {Function} fn The function to look for in the subscribers list
    * @param {Object} obj The execution scope Object for the subscription
    * @return {Boolean} true, if the function/Object pair is already subscribed 
    * to the CustomEvent passed in
    */
    Config.alreadySubscribed = function (evt, fn, obj) {
    
        var nSubscribers = evt.subscribers.length,
            subsc,
            i;

        if (nSubscribers > 0) {
            i = nSubscribers - 1;
            do {
                subsc = evt.subscribers[i];
                if (subsc && subsc.obj == obj && subsc.fn == fn) {
                    return true;
                }
            }
            while (i--);
        }

        return false;

    };

    YAHOO.lang.augmentProto(Config, YAHOO.util.EventProvider);

}());

/**
* YAHOO.widget.DateMath is used for simple date manipulation. The class is a static utility
* used for adding, subtracting, and comparing dates.
* @namespace YAHOO.widget
* @class DateMath
*/
YAHOO.widget.DateMath = {
	/**
	* Constant field representing Day
	* @property DAY
	* @static
	* @final
	* @type String
	*/
	DAY : "D",

	/**
	* Constant field representing Week
	* @property WEEK
	* @static
	* @final
	* @type String
	*/
	WEEK : "W",

	/**
	* Constant field representing Year
	* @property YEAR
	* @static
	* @final
	* @type String
	*/
	YEAR : "Y",

	/**
	* Constant field representing Month
	* @property MONTH
	* @static
	* @final
	* @type String
	*/
	MONTH : "M",

	/**
	* Constant field representing one day, in milliseconds
	* @property ONE_DAY_MS
	* @static
	* @final
	* @type Number
	*/
	ONE_DAY_MS : 1000*60*60*24,

	/**
	* Adds the specified amount of time to the this instance.
	* @method add
	* @param {Date} date	The JavaScript Date object to perform addition on
	* @param {String} field	The field constant to be used for performing addition.
	* @param {Number} amount	The number of units (measured in the field constant) to add to the date.
	* @return {Date} The resulting Date object
	*/
	add : function(date, field, amount) {
		var d = new Date(date.getTime());
		switch (field) {
			case this.MONTH:
				var newMonth = date.getMonth() + amount;
				var years = 0;


				if (newMonth < 0) {
					while (newMonth < 0) {
						newMonth += 12;
						years -= 1;
					}
				} else if (newMonth > 11) {
					while (newMonth > 11) {
						newMonth -= 12;
						years += 1;
					}
				}
				
				d.setMonth(newMonth);
				d.setFullYear(date.getFullYear() + years);
				break;
			case this.DAY:
				d.setDate(date.getDate() + amount);
				break;
			case this.YEAR:
				d.setFullYear(date.getFullYear() + amount);
				break;
			case this.WEEK:
				d.setDate(date.getDate() + (amount * 7));
				break;
		}
		return d;
	},

	/**
	* Subtracts the specified amount of time from the this instance.
	* @method subtract
	* @param {Date} date	The JavaScript Date object to perform subtraction on
	* @param {Number} field	The this field constant to be used for performing subtraction.
	* @param {Number} amount	The number of units (measured in the field constant) to subtract from the date.
	* @return {Date} The resulting Date object
	*/
	subtract : function(date, field, amount) {
		return this.add(date, field, (amount*-1));
	},

	/**
	* Determines whether a given date is before another date on the calendar.
	* @method before
	* @param {Date} date		The Date object to compare with the compare argument
	* @param {Date} compareTo	The Date object to use for the comparison
	* @return {Boolean} true if the date occurs before the compared date; false if not.
	*/
	before : function(date, compareTo) {
		var ms = compareTo.getTime();
		if (date.getTime() < ms) {
			return true;
		} else {
			return false;
		}
	},

	/**
	* Determines whether a given date is after another date on the calendar.
	* @method after
	* @param {Date} date		The Date object to compare with the compare argument
	* @param {Date} compareTo	The Date object to use for the comparison
	* @return {Boolean} true if the date occurs after the compared date; false if not.
	*/
	after : function(date, compareTo) {
		var ms = compareTo.getTime();
		if (date.getTime() > ms) {
			return true;
		} else {
			return false;
		}
	},

	/**
	* Determines whether a given date is between two other dates on the calendar.
	* @method between
	* @param {Date} date		The date to check for
	* @param {Date} dateBegin	The start of the range
	* @param {Date} dateEnd		The end of the range
	* @return {Boolean} true if the date occurs between the compared dates; false if not.
	*/
	between : function(date, dateBegin, dateEnd) {
		if (this.after(date, dateBegin) && this.before(date, dateEnd)) {
			return true;
		} else {
			return false;
		}
	},
	
	/**
	* Retrieves a JavaScript Date object representing January 1 of any given year.
	* @method getJan1
	* @param {Number} calendarYear		The calendar year for which to retrieve January 1
	* @return {Date}	January 1 of the calendar year specified.
	*/
	getJan1 : function(calendarYear) {
		return this.getDate(calendarYear,0,1);
	},

	/**
	* Calculates the number of days the specified date is from January 1 of the specified calendar year.
	* Passing January 1 to this function would return an offset value of zero.
	* @method getDayOffset
	* @param {Date}	date	The JavaScript date for which to find the offset
	* @param {Number} calendarYear	The calendar year to use for determining the offset
	* @return {Number}	The number of days since January 1 of the given year
	*/
	getDayOffset : function(date, calendarYear) {
		var beginYear = this.getJan1(calendarYear); // Find the start of the year. This will be in week 1.
		
		// Find the number of days the passed in date is away from the calendar year start
		var dayOffset = Math.ceil((date.getTime()-beginYear.getTime()) / this.ONE_DAY_MS);
		return dayOffset;
	},

	/**
	* Calculates the week number for the given date. This function assumes that week 1 is the
	* week in which January 1 appears, regardless of whether the week consists of a full 7 days.
	* The calendar year can be specified to help find what a the week number would be for a given
	* date if the date overlaps years. For instance, a week may be considered week 1 of 2005, or
	* week 53 of 2004. Specifying the optional calendarYear allows one to make this distinction
	* easily.
	* @method getWeekNumber
	* @param {Date}	date	The JavaScript date for which to find the week number
	* @param {Number} calendarYear	OPTIONAL - The calendar year to use for determining the week number. Default is
	*											the calendar year of parameter "date".
	* @return {Number}	The week number of the given date.
	*/
	getWeekNumber : function(date, calendarYear) {
		date = this.clearTime(date);
		var nearestThurs = new Date(date.getTime() + (4 * this.ONE_DAY_MS) - ((date.getDay()) * this.ONE_DAY_MS));

		var jan1 = this.getDate(nearestThurs.getFullYear(),0,1);
		var dayOfYear = ((nearestThurs.getTime() - jan1.getTime()) / this.ONE_DAY_MS) - 1;

		var weekNum = Math.ceil((dayOfYear)/ 7);
		return weekNum;
	},

	/**
	* Determines if a given week overlaps two different years.
	* @method isYearOverlapWeek
	* @param {Date}	weekBeginDate	The JavaScript Date representing the first day of the week.
	* @return {Boolean}	true if the date overlaps two different years.
	*/
	isYearOverlapWeek : function(weekBeginDate) {
		var overlaps = false;
		var nextWeek = this.add(weekBeginDate, this.DAY, 6);
		if (nextWeek.getFullYear() != weekBeginDate.getFullYear()) {
			overlaps = true;
		}
		return overlaps;
	},

	/**
	* Determines if a given week overlaps two different months.
	* @method isMonthOverlapWeek
	* @param {Date}	weekBeginDate	The JavaScript Date representing the first day of the week.
	* @return {Boolean}	true if the date overlaps two different months.
	*/
	isMonthOverlapWeek : function(weekBeginDate) {
		var overlaps = false;
		var nextWeek = this.add(weekBeginDate, this.DAY, 6);
		if (nextWeek.getMonth() != weekBeginDate.getMonth()) {
			overlaps = true;
		}
		return overlaps;
	},

	/**
	* Gets the first day of a month containing a given date.
	* @method findMonthStart
	* @param {Date}	date	The JavaScript Date used to calculate the month start
	* @return {Date}		The JavaScript Date representing the first day of the month
	*/
	findMonthStart : function(date) {
		var start = this.getDate(date.getFullYear(), date.getMonth(), 1);
		return start;
	},

	/**
	* Gets the last day of a month containing a given date.
	* @method findMonthEnd
	* @param {Date}	date	The JavaScript Date used to calculate the month end
	* @return {Date}		The JavaScript Date representing the last day of the month
	*/
	findMonthEnd : function(date) {
		var start = this.findMonthStart(date);
		var nextMonth = this.add(start, this.MONTH, 1);
		var end = this.subtract(nextMonth, this.DAY, 1);
		return end;
	},

	/**
	* Clears the time fields from a given date, effectively setting the time to 12 noon.
	* @method clearTime
	* @param {Date}	date	The JavaScript Date for which the time fields will be cleared
	* @return {Date}		The JavaScript Date cleared of all time fields
	*/
	clearTime : function(date) {
		date.setHours(12,0,0,0);
		return date;
	},

	/**
	 * Returns a new JavaScript Date object, representing the given year, month and date. Time fields (hr, min, sec, ms) on the new Date object
	 * are set to 0. The method allows Date instances to be created with the a year less than 100. "new Date(year, month, date)" implementations 
	 * set the year to 19xx if a year (xx) which is less than 100 is provided.
	 * <p>
	 * <em>NOTE:</em>Validation on argument values is not performed. It is the caller's responsibility to ensure
	 * arguments are valid as per the ECMAScript-262 Date object specification for the new Date(year, month[, date]) constructor.
	 * </p>
	 * @method getDate
	 * @param {Number} y Year.
	 * @param {Number} m Month index from 0 (Jan) to 11 (Dec).
	 * @param {Number} d (optional) Date from 1 to 31. If not provided, defaults to 1.
	 * @return {Date} The JavaScript date object with year, month, date set as provided.
	 */
	getDate : function(y, m, d) {
		var dt = null;
		if (YAHOO.lang.isUndefined(d)) {
			d = 1;
		}
		if (y >= 100) {
			dt = new Date(y, m, d);
		} else {
			dt = new Date();
			dt.setFullYear(y);
			dt.setMonth(m);
			dt.setDate(d);
			dt.setHours(0,0,0,0);
		}
		return dt;
	}
};

/**
* The Calendar component is a UI control that enables users to choose one or more dates from a graphical calendar presented in a one-month or
* multi-month interface. Calendars are generated entirely via script and can be navigated without any page refreshes.
* @module    calendar
* @title    Calendar
* @namespace  YAHOO.widget
* @requires  yahoo,dom,event
*/

/**
* Calendar is the base class for the Calendar widget. In its most basic
* implementation, it has the ability to render a calendar widget on the page
* that can be manipulated to select a single date, move back and forth between
* months and years.
* <p>To construct the placeholder for the calendar widget, the code is as
* follows:
*	<xmp>
*		<div id="calContainer"></div>
*	</xmp>
* </p>
* <p>
* <strong>NOTE: As of 2.4.0, the constructor's ID argument is optional.</strong>
* The Calendar can be constructed by simply providing a container ID string, 
* or a reference to a container DIV HTMLElement (the element needs to exist 
* in the document).
* 
* E.g.:
*	<xmp>
*		var c = new YAHOO.widget.Calendar("calContainer", configOptions);
*	</xmp>
* or:
*   <xmp>
*       var containerDiv = YAHOO.util.Dom.get("calContainer");
*		var c = new YAHOO.widget.Calendar(containerDiv, configOptions);
*	</xmp>
* </p>
* <p>
* If not provided, the ID will be generated from the container DIV ID by adding an "_t" suffix.
* For example if an ID is not provided, and the container's ID is "calContainer", the Calendar's ID will be set to "calContainer_t".
* </p>
* 
* @namespace YAHOO.widget
* @class Calendar
* @constructor
* @param {String} id optional The id of the table element that will represent the Calendar widget. As of 2.4.0, this argument is optional.
* @param {String | HTMLElement} container The id of the container div element that will wrap the Calendar table, or a reference to a DIV element which exists in the document.
* @param {Object} config optional The configuration object containing the initial configuration values for the Calendar.
*/
YAHOO.widget.Calendar = function(id, containerId, config) {
	this.init.apply(this, arguments);
};

/**
* The path to be used for images loaded for the Calendar
* @property YAHOO.widget.Calendar.IMG_ROOT
* @static
* @deprecated	You can now customize images by overriding the calclose, calnavleft and calnavright default CSS classes for the close icon, left arrow and right arrow respectively
* @type String
*/
YAHOO.widget.Calendar.IMG_ROOT = null;

/**
* Type constant used for renderers to represent an individual date (M/D/Y)
* @property YAHOO.widget.Calendar.DATE
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.DATE = "D";

/**
* Type constant used for renderers to represent an individual date across any year (M/D)
* @property YAHOO.widget.Calendar.MONTH_DAY
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.MONTH_DAY = "MD";

/**
* Type constant used for renderers to represent a weekday
* @property YAHOO.widget.Calendar.WEEKDAY
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.WEEKDAY = "WD";

/**
* Type constant used for renderers to represent a range of individual dates (M/D/Y-M/D/Y)
* @property YAHOO.widget.Calendar.RANGE
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.RANGE = "R";

/**
* Type constant used for renderers to represent a month across any year
* @property YAHOO.widget.Calendar.MONTH
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.MONTH = "M";

/**
* Constant that represents the total number of date cells that are displayed in a given month
* @property YAHOO.widget.Calendar.DISPLAY_DAYS
* @static
* @final
* @type Number
*/
YAHOO.widget.Calendar.DISPLAY_DAYS = 42;

/**
* Constant used for halting the execution of the remainder of the render stack
* @property YAHOO.widget.Calendar.STOP_RENDER
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.STOP_RENDER = "S";

/**
* Constant used to represent short date field string formats (e.g. Tu or Feb)
* @property YAHOO.widget.Calendar.SHORT
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.SHORT = "short";

/**
* Constant used to represent long date field string formats (e.g. Monday or February)
* @property YAHOO.widget.Calendar.LONG
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.LONG = "long";

/**
* Constant used to represent medium date field string formats (e.g. Mon)
* @property YAHOO.widget.Calendar.MEDIUM
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.MEDIUM = "medium";

/**
* Constant used to represent single character date field string formats (e.g. M, T, W)
* @property YAHOO.widget.Calendar.ONE_CHAR
* @static
* @final
* @type String
*/
YAHOO.widget.Calendar.ONE_CHAR = "1char";

/**
* The set of default Config property keys and values for the Calendar
* @property YAHOO.widget.Calendar._DEFAULT_CONFIG
* @final
* @static
* @private
* @type Object
*/
YAHOO.widget.Calendar._DEFAULT_CONFIG = {
	// Default values for pagedate and selected are not class level constants - they are set during instance creation 
	PAGEDATE : {key:"pagedate", value:null},
	SELECTED : {key:"selected", value:null},
	TITLE : {key:"title", value:""},
	CLOSE : {key:"close", value:false},
	IFRAME : {key:"iframe", value:(YAHOO.env.ua.ie && YAHOO.env.ua.ie <= 6) ? true : false},
	MINDATE : {key:"mindate", value:null},
	MAXDATE : {key:"maxdate", value:null},
	MULTI_SELECT : {key:"multi_select", value:false},
	START_WEEKDAY : {key:"start_weekday", value:0},
	SHOW_WEEKDAYS : {key:"show_weekdays", value:true},
	SHOW_WEEK_HEADER : {key:"show_week_header", value:false},
	SHOW_WEEK_FOOTER : {key:"show_week_footer", value:false},
	HIDE_BLANK_WEEKS : {key:"hide_blank_weeks", value:false},
	NAV_ARROW_LEFT: {key:"nav_arrow_left", value:null} ,
	NAV_ARROW_RIGHT : {key:"nav_arrow_right", value:null} ,
	MONTHS_SHORT : {key:"months_short", value:["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]},
	MONTHS_LONG: {key:"months_long", value:["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"]},
	WEEKDAYS_1CHAR: {key:"weekdays_1char", value:["S", "M", "T", "W", "T", "F", "S"]},
	WEEKDAYS_SHORT: {key:"weekdays_short", value:["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"]},
	WEEKDAYS_MEDIUM: {key:"weekdays_medium", value:["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]},
	WEEKDAYS_LONG: {key:"weekdays_long", value:["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]},
	LOCALE_MONTHS:{key:"locale_months", value:"long"},
	LOCALE_WEEKDAYS:{key:"locale_weekdays", value:"short"},
	DATE_DELIMITER:{key:"date_delimiter", value:","},
	DATE_FIELD_DELIMITER:{key:"date_field_delimiter", value:"/"},
	DATE_RANGE_DELIMITER:{key:"date_range_delimiter", value:"-"},
	MY_MONTH_POSITION:{key:"my_month_position", value:1},
	MY_YEAR_POSITION:{key:"my_year_position", value:2},
	MD_MONTH_POSITION:{key:"md_month_position", value:1},
	MD_DAY_POSITION:{key:"md_day_position", value:2},
	MDY_MONTH_POSITION:{key:"mdy_month_position", value:1},
	MDY_DAY_POSITION:{key:"mdy_day_position", value:2},
	MDY_YEAR_POSITION:{key:"mdy_year_position", value:3},
	MY_LABEL_MONTH_POSITION:{key:"my_label_month_position", value:1},
	MY_LABEL_YEAR_POSITION:{key:"my_label_year_position", value:2},
	MY_LABEL_MONTH_SUFFIX:{key:"my_label_month_suffix", value:" "},
	MY_LABEL_YEAR_SUFFIX:{key:"my_label_year_suffix", value:""},
	NAV: {key:"navigator", value: null}
};

/**
* The set of Custom Event types supported by the Calendar
* @property YAHOO.widget.Calendar._EVENT_TYPES
* @final
* @static
* @private
* @type Object
*/
YAHOO.widget.Calendar._EVENT_TYPES = {
	BEFORE_SELECT : "beforeSelect", 
	SELECT : "select",
	BEFORE_DESELECT : "beforeDeselect",
	DESELECT : "deselect",
	CHANGE_PAGE : "changePage",
	BEFORE_RENDER : "beforeRender",
	RENDER : "render",
	RESET : "reset",
	CLEAR : "clear",
	BEFORE_HIDE : "beforeHide",
	HIDE : "hide",
	BEFORE_SHOW : "beforeShow",
	SHOW : "show",
	BEFORE_HIDE_NAV : "beforeHideNav",
	HIDE_NAV : "hideNav",
	BEFORE_SHOW_NAV : "beforeShowNav",
	SHOW_NAV : "showNav",
	BEFORE_RENDER_NAV : "beforeRenderNav",
	RENDER_NAV : "renderNav"
};

/**
* The set of default style constants for the Calendar
* @property YAHOO.widget.Calendar._STYLES
* @final
* @static
* @private
* @type Object
*/
YAHOO.widget.Calendar._STYLES = {
	CSS_ROW_HEADER: "calrowhead",
	CSS_ROW_FOOTER: "calrowfoot",
	CSS_CELL : "calcell",
	CSS_CELL_SELECTOR : "selector",
	CSS_CELL_SELECTED : "selected",
	CSS_CELL_SELECTABLE : "selectable",
	CSS_CELL_RESTRICTED : "restricted",
	CSS_CELL_TODAY : "today",
	CSS_CELL_OOM : "oom",
	CSS_CELL_OOB : "previous",
	CSS_HEADER : "calheader",
	CSS_HEADER_TEXT : "calhead",
	CSS_BODY : "calbody",
	CSS_WEEKDAY_CELL : "calweekdaycell",
	CSS_WEEKDAY_ROW : "calweekdayrow",
	CSS_FOOTER : "calfoot",
	CSS_CALENDAR : "yui-calendar",
	CSS_SINGLE : "single",
	CSS_CONTAINER : "yui-calcontainer",
	CSS_NAV_LEFT : "calnavleft",
	CSS_NAV_RIGHT : "calnavright",
	CSS_NAV : "calnav",
	CSS_CLOSE : "calclose",
	CSS_CELL_TOP : "calcelltop",
	CSS_CELL_LEFT : "calcellleft",
	CSS_CELL_RIGHT : "calcellright",
	CSS_CELL_BOTTOM : "calcellbottom",
	CSS_CELL_HOVER : "calcellhover",
	CSS_CELL_HIGHLIGHT1 : "highlight1",
	CSS_CELL_HIGHLIGHT2 : "highlight2",
	CSS_CELL_HIGHLIGHT3 : "highlight3",
	CSS_CELL_HIGHLIGHT4 : "highlight4"
};

YAHOO.widget.Calendar.prototype = {

	/**
	* The configuration object used to set up the calendars various locale and style options.
	* @property Config
	* @private
	* @deprecated Configuration properties should be set by calling Calendar.cfg.setProperty.
	* @type Object
	*/
	Config : null,

	/**
	* The parent CalendarGroup, only to be set explicitly by the parent group
	* @property parent
	* @type CalendarGroup
	*/	
	parent : null,

	/**
	* The index of this item in the parent group
	* @property index
	* @type Number
	*/
	index : -1,

	/**
	* The collection of calendar table cells
	* @property cells
	* @type HTMLTableCellElement[]
	*/
	cells : null,

	/**
	* The collection of calendar cell dates that is parallel to the cells collection. The array contains dates field arrays in the format of [YYYY, M, D].
	* @property cellDates
	* @type Array[](Number[])
	*/
	cellDates : null,

	/**
	* The id that uniquely identifies this Calendar.
	* @property id
	* @type String
	*/
	id : null,

	/**
	* The unique id associated with the Calendar's container
	* @property containerId
	* @type String
	*/
	containerId: null,

	/**
	* The DOM element reference that points to this calendar's container element. The calendar will be inserted into this element when the shell is rendered.
	* @property oDomContainer
	* @type HTMLElement
	*/
	oDomContainer : null,

	/**
	* A Date object representing today's date.
	* @property today
	* @type Date
	*/
	today : null,

	/**
	* The list of render functions, along with required parameters, used to render cells. 
	* @property renderStack
	* @type Array[]
	*/
	renderStack : null,

	/**
	* A copy of the initial render functions created before rendering.
	* @property _renderStack
	* @private
	* @type Array
	*/
	_renderStack : null,

	/**
	* A reference to the CalendarNavigator instance created for this Calendar.
	* Will be null if the "navigator" configuration property has not been set
	* @property oNavigator
	* @type CalendarNavigator
	*/
	oNavigator : null,

	/**
	* The private list of initially selected dates.
	* @property _selectedDates
	* @private
	* @type Array
	*/
	_selectedDates : null,

	/**
	* A map of DOM event handlers to attach to cells associated with specific CSS class names
	* @property domEventMap
	* @type Object
	*/
	domEventMap : null,

	/**
	 * Protected helper used to parse Calendar constructor/init arguments.
	 *
	 * As of 2.4.0, Calendar supports a simpler constructor 
	 * signature. This method reconciles arguments
	 * received in the pre 2.4.0 and 2.4.0 formats.
	 * 
	 * @protected
	 * @method _parseArgs
	 * @param {Array} Function "arguments" array
	 * @return {Object} Object with id, container, config properties containing
	 * the reconciled argument values.
	 **/
	_parseArgs : function(args) {
		/*
		   2.4.0 Constructors signatures

		   new Calendar(String)
		   new Calendar(HTMLElement)
		   new Calendar(String, ConfigObject)
		   new Calendar(HTMLElement, ConfigObject)

		   Pre 2.4.0 Constructor signatures

		   new Calendar(String, String)
		   new Calendar(String, HTMLElement)
		   new Calendar(String, String, ConfigObject)
		   new Calendar(String, HTMLElement, ConfigObject)
		 */
		var nArgs = {id:null, container:null, config:null};

		if (args && args.length && args.length > 0) {
			switch (args.length) {
				case 1:
					nArgs.id = null;
					nArgs.container = args[0];
					nArgs.config = null;
					break;
				case 2:
					if (YAHOO.lang.isObject(args[1]) && !args[1].tagName && !(args[1] instanceof String)) {
						nArgs.id = null;
						nArgs.container = args[0];
						nArgs.config = args[1];
					} else {
						nArgs.id = args[0];
						nArgs.container = args[1];
						nArgs.config = null;
					}
					break;
				default: // 3+
					nArgs.id = args[0];
					nArgs.container = args[1];
					nArgs.config = args[2];
					break;
			}
		} else {
		}
		return nArgs;
	},

	/**
	* Initializes the Calendar widget.
	* @method init
	*
	* @param {String} id optional The id of the table element that will represent the Calendar widget. As of 2.4.0, this argument is optional.
	* @param {String | HTMLElement} container The id of the container div element that will wrap the Calendar table, or a reference to a DIV element which exists in the document.
	* @param {Object} config optional The configuration object containing the initial configuration values for the Calendar.
	*/
	init : function(id, container, config) {
		// Normalize 2.4.0, pre 2.4.0 args
		var nArgs = this._parseArgs(arguments);

		id = nArgs.id;
		container = nArgs.container;
		config = nArgs.config;

		this.oDomContainer = YAHOO.util.Dom.get(container);

		if (!this.oDomContainer.id) {
			this.oDomContainer.id = YAHOO.util.Dom.generateId();
		}
		if (!id) {
			id = this.oDomContainer.id + "_t";
		}

		this.id = id;
		this.containerId = this.oDomContainer.id;

		this.initEvents();

		this.today = new Date();
		YAHOO.widget.DateMath.clearTime(this.today);

		/**
		* The Config object used to hold the configuration variables for the Calendar
		* @property cfg
		* @type YAHOO.util.Config
		*/
		this.cfg = new YAHOO.util.Config(this);

		/**
		* The local object which contains the Calendar's options
		* @property Options
		* @type Object
		*/
		this.Options = {};

		/**
		* The local object which contains the Calendar's locale settings
		* @property Locale
		* @type Object
		*/
		this.Locale = {};

		this.initStyles();

		YAHOO.util.Dom.addClass(this.oDomContainer, this.Style.CSS_CONTAINER);
		YAHOO.util.Dom.addClass(this.oDomContainer, this.Style.CSS_SINGLE);

		this.cellDates = [];
		this.cells = [];
		this.renderStack = [];
		this._renderStack = [];

		this.setupConfig();

		if (config) {
			this.cfg.applyConfig(config, true);
		}

		this.cfg.fireQueue();
	},

	/**
	* Default Config listener for the iframe property. If the iframe config property is set to true, 
	* renders the built-in IFRAME shim if the container is relatively or absolutely positioned.
	* 
	* @method configIframe
	*/
	configIframe : function(type, args, obj) {
		var useIframe = args[0];
	
		if (!this.parent) {
			if (YAHOO.util.Dom.inDocument(this.oDomContainer)) {
				if (useIframe) {
					var pos = YAHOO.util.Dom.getStyle(this.oDomContainer, "position");
					
					if (pos == "absolute" || pos == "relative") {
						
						if (!YAHOO.util.Dom.inDocument(this.iframe)) {
							this.iframe = document.createElement("iframe");
							this.iframe.src = "javascript:false;";
	
							YAHOO.util.Dom.setStyle(this.iframe, "opacity", "0");
	
							if (YAHOO.env.ua.ie && YAHOO.env.ua.ie <= 6) {
								YAHOO.util.Dom.addClass(this.iframe, "fixedsize");
							}
	
							this.oDomContainer.insertBefore(this.iframe, this.oDomContainer.firstChild);
						}
					}
				} else {
					if (this.iframe) {
						if (this.iframe.parentNode) {
							this.iframe.parentNode.removeChild(this.iframe);
						}
						this.iframe = null;
					}
				}
			}
		}
	},

	/**
	* Default handler for the "title" property
	* @method configTitle
	*/
	configTitle : function(type, args, obj) {
		var title = args[0];

		// "" disables title bar
		if (title) {
			this.createTitleBar(title);
		} else {
			var close = this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.CLOSE.key);
			if (!close) {
				this.removeTitleBar();
			} else {
				this.createTitleBar("&#160;");
			}
		}
	},
	
	/**
	* Default handler for the "close" property
	* @method configClose
	*/
	configClose : function(type, args, obj) {
		var close = args[0],
			title = this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.TITLE.key);
	
		if (close) {
			if (!title) {
				this.createTitleBar("&#160;");
			}
			this.createCloseButton();
		} else {
			this.removeCloseButton();
			if (!title) {
				this.removeTitleBar();
			}
		}
	},
	
	/**
	* Initializes Calendar's built-in CustomEvents
	* @method initEvents
	*/
	initEvents : function() {
	
		var defEvents = YAHOO.widget.Calendar._EVENT_TYPES;
	
		/**
		* Fired before a selection is made
		* @event beforeSelectEvent
		*/
		this.beforeSelectEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SELECT); 
	
		/**
		* Fired when a selection is made
		* @event selectEvent
		* @param {Array}	Array of Date field arrays in the format [YYYY, MM, DD].
		*/
		this.selectEvent = new YAHOO.util.CustomEvent(defEvents.SELECT);
	
		/**
		* Fired before a selection is made
		* @event beforeDeselectEvent
		*/
		this.beforeDeselectEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_DESELECT);
	
		/**
		* Fired when a selection is made
		* @event deselectEvent
		* @param {Array}	Array of Date field arrays in the format [YYYY, MM, DD].
		*/
		this.deselectEvent = new YAHOO.util.CustomEvent(defEvents.DESELECT);
	
		/**
		* Fired when the Calendar page is changed
		* @event changePageEvent
		*/
		this.changePageEvent = new YAHOO.util.CustomEvent(defEvents.CHANGE_PAGE);
	
		/**
		* Fired before the Calendar is rendered
		* @event beforeRenderEvent
		*/
		this.beforeRenderEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_RENDER);
	
		/**
		* Fired when the Calendar is rendered
		* @event renderEvent
		*/
		this.renderEvent = new YAHOO.util.CustomEvent(defEvents.RENDER);
	
		/**
		* Fired when the Calendar is reset
		* @event resetEvent
		*/
		this.resetEvent = new YAHOO.util.CustomEvent(defEvents.RESET);
	
		/**
		* Fired when the Calendar is cleared
		* @event clearEvent
		*/
		this.clearEvent = new YAHOO.util.CustomEvent(defEvents.CLEAR);
	
		/**
		* Fired just before the Calendar is to be shown
		* @event beforeShowEvent
		*/
		this.beforeShowEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SHOW);
	
		/**
		* Fired after the Calendar is shown
		* @event showEvent
		*/
		this.showEvent = new YAHOO.util.CustomEvent(defEvents.SHOW);
	
		/**
		* Fired just before the Calendar is to be hidden
		* @event beforeHideEvent
		*/
		this.beforeHideEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_HIDE);
	
		/**
		* Fired after the Calendar is hidden
		* @event hideEvent
		*/
		this.hideEvent = new YAHOO.util.CustomEvent(defEvents.HIDE);

		/**
		* Fired just before the CalendarNavigator is to be shown
		* @event beforeShowNavEvent
		*/
		this.beforeShowNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SHOW_NAV);
	
		/**
		* Fired after the CalendarNavigator is shown
		* @event showNavEvent
		*/
		this.showNavEvent = new YAHOO.util.CustomEvent(defEvents.SHOW_NAV);
	
		/**
		* Fired just before the CalendarNavigator is to be hidden
		* @event beforeHideNavEvent
		*/
		this.beforeHideNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_HIDE_NAV);
	
		/**
		* Fired after the CalendarNavigator is hidden
		* @event hideNavEvent
		*/
		this.hideNavEvent = new YAHOO.util.CustomEvent(defEvents.HIDE_NAV);

		/**
		* Fired just before the CalendarNavigator is to be rendered
		* @event beforeRenderNavEvent
		*/
		this.beforeRenderNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_RENDER_NAV);

		/**
		* Fired after the CalendarNavigator is rendered
		* @event renderNavEvent
		*/
		this.renderNavEvent = new YAHOO.util.CustomEvent(defEvents.RENDER_NAV);

		this.beforeSelectEvent.subscribe(this.onBeforeSelect, this, true);
		this.selectEvent.subscribe(this.onSelect, this, true);
		this.beforeDeselectEvent.subscribe(this.onBeforeDeselect, this, true);
		this.deselectEvent.subscribe(this.onDeselect, this, true);
		this.changePageEvent.subscribe(this.onChangePage, this, true);
		this.renderEvent.subscribe(this.onRender, this, true);
		this.resetEvent.subscribe(this.onReset, this, true);
		this.clearEvent.subscribe(this.onClear, this, true);
	},
	
	/**
	* The default event function that is attached to a date link within a calendar cell
	* when the calendar is rendered.
	* @method doSelectCell
	* @param {DOMEvent} e	The event
	* @param {Calendar} cal	A reference to the calendar passed by the Event utility
	*/
	doSelectCell : function(e, cal) {
		var cell,index,d,date;

		var target = YAHOO.util.Event.getTarget(e);
		var tagName = target.tagName.toLowerCase();
		var defSelector = false;

		while (tagName != "td" && ! YAHOO.util.Dom.hasClass(target, cal.Style.CSS_CELL_SELECTABLE)) {

			if (!defSelector && tagName == "a" && YAHOO.util.Dom.hasClass(target, cal.Style.CSS_CELL_SELECTOR)) {
				defSelector = true;	
			}

			target = target.parentNode;
			tagName = target.tagName.toLowerCase();
			// TODO: No need to go all the way up to html.
			if (tagName == "html") {
				return;
			}
		}

		if (defSelector) {
			// Stop link href navigation for default renderer
			YAHOO.util.Event.preventDefault(e);
		}
	
		cell = target;

		if (YAHOO.util.Dom.hasClass(cell, cal.Style.CSS_CELL_SELECTABLE)) {
			index = cell.id.split("cell")[1];
			d = cal.cellDates[index];
			date = YAHOO.widget.DateMath.getDate(d[0],d[1]-1,d[2]);
		
			var link;

			if (cal.Options.MULTI_SELECT) {
				link = cell.getElementsByTagName("a")[0];
				if (link) {
					link.blur();
				}

				var cellDate = cal.cellDates[index];
				var cellDateIndex = cal._indexOfSelectedFieldArray(cellDate);

				if (cellDateIndex > -1) {	
					cal.deselectCell(index);
				} else {
					cal.selectCell(index);
				}	
	
			} else {
				link = cell.getElementsByTagName("a")[0];
				if (link) {
					link.blur();
				}
				cal.selectCell(index);
			}
		}
	},

	/**
	* The event that is executed when the user hovers over a cell
	* @method doCellMouseOver
	* @param {DOMEvent} e	The event
	* @param {Calendar} cal	A reference to the calendar passed by the Event utility
	*/
	doCellMouseOver : function(e, cal) {
		var target;
		if (e) {
			target = YAHOO.util.Event.getTarget(e);
		} else {
			target = this;
		}

		while (target.tagName && target.tagName.toLowerCase() != "td") {
			target = target.parentNode;
			if (!target.tagName || target.tagName.toLowerCase() == "html") {
				return;
			}
		}

		if (YAHOO.util.Dom.hasClass(target, cal.Style.CSS_CELL_SELECTABLE)) {
			YAHOO.util.Dom.addClass(target, cal.Style.CSS_CELL_HOVER);
		}
	},

	/**
	* The event that is executed when the user moves the mouse out of a cell
	* @method doCellMouseOut
	* @param {DOMEvent} e	The event
	* @param {Calendar} cal	A reference to the calendar passed by the Event utility
	*/
	doCellMouseOut : function(e, cal) {
		var target;
		if (e) {
			target = YAHOO.util.Event.getTarget(e);
		} else {
			target = this;
		}

		while (target.tagName && target.tagName.toLowerCase() != "td") {
			target = target.parentNode;
			if (!target.tagName || target.tagName.toLowerCase() == "html") {
				return;
			}
		}

		if (YAHOO.util.Dom.hasClass(target, cal.Style.CSS_CELL_SELECTABLE)) {
			YAHOO.util.Dom.removeClass(target, cal.Style.CSS_CELL_HOVER);
		}
	},
	
	setupConfig : function() {
	
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;

		/**
		* The month/year representing the current visible Calendar date (mm/yyyy)
		* @config pagedate
		* @type String
		* @default today's date
		*/
		this.cfg.addProperty(defCfg.PAGEDATE.key, { value:new Date(), handler:this.configPageDate } );

		/**
		* The date or range of dates representing the current Calendar selection
		* @config selected
		* @type String
		* @default []
		*/
		this.cfg.addProperty(defCfg.SELECTED.key, { value:[], handler:this.configSelected } );

		/**
		* The title to display above the Calendar's month header
		* @config title
		* @type String
		* @default ""
		*/
		this.cfg.addProperty(defCfg.TITLE.key, { value:defCfg.TITLE.value, handler:this.configTitle } );

		/**
		* Whether or not a close button should be displayed for this Calendar
		* @config close
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.CLOSE.key, { value:defCfg.CLOSE.value, handler:this.configClose } );

		/**
		* Whether or not an iframe shim should be placed under the Calendar to prevent select boxes from bleeding through in Internet Explorer 6 and below.
		* This property is enabled by default for IE6 and below. It is disabled by default for other browsers for performance reasons, but can be 
		* enabled if required.
		* 
		* @config iframe
		* @type Boolean
		* @default true for IE6 and below, false for all other browsers
		*/
		this.cfg.addProperty(defCfg.IFRAME.key, { value:defCfg.IFRAME.value, handler:this.configIframe, validator:this.cfg.checkBoolean } );

		/**
		* The minimum selectable date in the current Calendar (mm/dd/yyyy)
		* @config mindate
		* @type String
		* @default null
		*/
		this.cfg.addProperty(defCfg.MINDATE.key, { value:defCfg.MINDATE.value, handler:this.configMinDate } );

		/**
		* The maximum selectable date in the current Calendar (mm/dd/yyyy)
		* @config maxdate
		* @type String
		* @default null
		*/
		this.cfg.addProperty(defCfg.MAXDATE.key, { value:defCfg.MAXDATE.value, handler:this.configMaxDate } );
	
	
		// Options properties
	
		/**
		* True if the Calendar should allow multiple selections. False by default.
		* @config MULTI_SELECT
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.MULTI_SELECT.key,	{ value:defCfg.MULTI_SELECT.value, handler:this.configOptions, validator:this.cfg.checkBoolean } );
	
		/**
		* The weekday the week begins on. Default is 0 (Sunday).
		* @config START_WEEKDAY
		* @type number
		* @default 0
		*/
		this.cfg.addProperty(defCfg.START_WEEKDAY.key,	{ value:defCfg.START_WEEKDAY.value, handler:this.configOptions, validator:this.cfg.checkNumber  } );
	
		/**
		* True if the Calendar should show weekday labels. True by default.
		* @config SHOW_WEEKDAYS
		* @type Boolean
		* @default true
		*/
		this.cfg.addProperty(defCfg.SHOW_WEEKDAYS.key,	{ value:defCfg.SHOW_WEEKDAYS.value, handler:this.configOptions, validator:this.cfg.checkBoolean  } );
	
		/**
		* True if the Calendar should show week row headers. False by default.
		* @config SHOW_WEEK_HEADER
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.SHOW_WEEK_HEADER.key, { value:defCfg.SHOW_WEEK_HEADER.value, handler:this.configOptions, validator:this.cfg.checkBoolean } );
	
		/**
		* True if the Calendar should show week row footers. False by default.
		* @config SHOW_WEEK_FOOTER
		* @type Boolean
		* @default false
		*/	
		this.cfg.addProperty(defCfg.SHOW_WEEK_FOOTER.key,{ value:defCfg.SHOW_WEEK_FOOTER.value, handler:this.configOptions, validator:this.cfg.checkBoolean } );
	
		/**
		* True if the Calendar should suppress weeks that are not a part of the current month. False by default.
		* @config HIDE_BLANK_WEEKS
		* @type Boolean
		* @default false
		*/	
		this.cfg.addProperty(defCfg.HIDE_BLANK_WEEKS.key, { value:defCfg.HIDE_BLANK_WEEKS.value, handler:this.configOptions, validator:this.cfg.checkBoolean } );
		
		/**
		* The image that should be used for the left navigation arrow.
		* @config NAV_ARROW_LEFT
		* @type String
		* @deprecated	You can customize the image by overriding the default CSS class for the left arrow - "calnavleft"  
		* @default null
		*/	
		this.cfg.addProperty(defCfg.NAV_ARROW_LEFT.key,	{ value:defCfg.NAV_ARROW_LEFT.value, handler:this.configOptions } );
	
		/**
		* The image that should be used for the right navigation arrow.
		* @config NAV_ARROW_RIGHT
		* @type String
		* @deprecated	You can customize the image by overriding the default CSS class for the right arrow - "calnavright"
		* @default null
		*/	
		this.cfg.addProperty(defCfg.NAV_ARROW_RIGHT.key, { value:defCfg.NAV_ARROW_RIGHT.value, handler:this.configOptions } );
	
		// Locale properties
	
		/**
		* The short month labels for the current locale.
		* @config MONTHS_SHORT
		* @type String[]
		* @default ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
		*/
		this.cfg.addProperty(defCfg.MONTHS_SHORT.key,	{ value:defCfg.MONTHS_SHORT.value, handler:this.configLocale } );
		
		/**
		* The long month labels for the current locale.
		* @config MONTHS_LONG
		* @type String[]
		* @default ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
		*/	
		this.cfg.addProperty(defCfg.MONTHS_LONG.key,		{ value:defCfg.MONTHS_LONG.value, handler:this.configLocale } );

		/**
		* The 1-character weekday labels for the current locale.
		* @config WEEKDAYS_1CHAR
		* @type String[]
		* @default ["S", "M", "T", "W", "T", "F", "S"]
		*/	
		this.cfg.addProperty(defCfg.WEEKDAYS_1CHAR.key,	{ value:defCfg.WEEKDAYS_1CHAR.value, handler:this.configLocale } );
		
		/**
		* The short weekday labels for the current locale.
		* @config WEEKDAYS_SHORT
		* @type String[]
		* @default ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"]
		*/	
		this.cfg.addProperty(defCfg.WEEKDAYS_SHORT.key,	{ value:defCfg.WEEKDAYS_SHORT.value, handler:this.configLocale } );
		
		/**
		* The medium weekday labels for the current locale.
		* @config WEEKDAYS_MEDIUM
		* @type String[]
		* @default ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
		*/	
		this.cfg.addProperty(defCfg.WEEKDAYS_MEDIUM.key,	{ value:defCfg.WEEKDAYS_MEDIUM.value, handler:this.configLocale } );
		
		/**
		* The long weekday labels for the current locale.
		* @config WEEKDAYS_LONG
		* @type String[]
		* @default ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
		*/	
		this.cfg.addProperty(defCfg.WEEKDAYS_LONG.key,	{ value:defCfg.WEEKDAYS_LONG.value, handler:this.configLocale } );
	
		/**
		* Refreshes the locale values used to build the Calendar.
		* @method refreshLocale
		* @private
		*/
		var refreshLocale = function() {
			this.cfg.refireEvent(defCfg.LOCALE_MONTHS.key);
			this.cfg.refireEvent(defCfg.LOCALE_WEEKDAYS.key);
		};
	
		this.cfg.subscribeToConfigEvent(defCfg.START_WEEKDAY.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.MONTHS_SHORT.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.MONTHS_LONG.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.WEEKDAYS_1CHAR.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.WEEKDAYS_SHORT.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.WEEKDAYS_MEDIUM.key, refreshLocale, this, true);
		this.cfg.subscribeToConfigEvent(defCfg.WEEKDAYS_LONG.key, refreshLocale, this, true);
		
		/**
		* The setting that determines which length of month labels should be used. Possible values are "short" and "long".
		* @config LOCALE_MONTHS
		* @type String
		* @default "long"
		*/	
		this.cfg.addProperty(defCfg.LOCALE_MONTHS.key,	{ value:defCfg.LOCALE_MONTHS.value, handler:this.configLocaleValues } );
		
		/**
		* The setting that determines which length of weekday labels should be used. Possible values are "1char", "short", "medium", and "long".
		* @config LOCALE_WEEKDAYS
		* @type String
		* @default "short"
		*/	
		this.cfg.addProperty(defCfg.LOCALE_WEEKDAYS.key,	{ value:defCfg.LOCALE_WEEKDAYS.value, handler:this.configLocaleValues } );
	
		/**
		* The value used to delimit individual dates in a date string passed to various Calendar functions.
		* @config DATE_DELIMITER
		* @type String
		* @default ","
		*/	
		this.cfg.addProperty(defCfg.DATE_DELIMITER.key,		{ value:defCfg.DATE_DELIMITER.value, handler:this.configLocale } );
	
		/**
		* The value used to delimit date fields in a date string passed to various Calendar functions.
		* @config DATE_FIELD_DELIMITER
		* @type String
		* @default "/"
		*/	
		this.cfg.addProperty(defCfg.DATE_FIELD_DELIMITER.key, { value:defCfg.DATE_FIELD_DELIMITER.value, handler:this.configLocale } );
	
		/**
		* The value used to delimit date ranges in a date string passed to various Calendar functions.
		* @config DATE_RANGE_DELIMITER
		* @type String
		* @default "-"
		*/
		this.cfg.addProperty(defCfg.DATE_RANGE_DELIMITER.key, { value:defCfg.DATE_RANGE_DELIMITER.value, handler:this.configLocale } );
	
		/**
		* The position of the month in a month/year date string
		* @config MY_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MY_MONTH_POSITION.key,	{ value:defCfg.MY_MONTH_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the year in a month/year date string
		* @config MY_YEAR_POSITION
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.MY_YEAR_POSITION.key,	{ value:defCfg.MY_YEAR_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the month in a month/day date string
		* @config MD_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MD_MONTH_POSITION.key,	{ value:defCfg.MD_MONTH_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the day in a month/year date string
		* @config MD_DAY_POSITION
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.MD_DAY_POSITION.key,		{ value:defCfg.MD_DAY_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the month in a month/day/year date string
		* @config MDY_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MDY_MONTH_POSITION.key,	{ value:defCfg.MDY_MONTH_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the day in a month/day/year date string
		* @config MDY_DAY_POSITION
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.MDY_DAY_POSITION.key,	{ value:defCfg.MDY_DAY_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the year in a month/day/year date string
		* @config MDY_YEAR_POSITION
		* @type Number
		* @default 3
		*/
		this.cfg.addProperty(defCfg.MDY_YEAR_POSITION.key,	{ value:defCfg.MDY_YEAR_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the month in the month year label string used as the Calendar header
		* @config MY_LABEL_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_MONTH_POSITION.key,	{ value:defCfg.MY_LABEL_MONTH_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the year in the month year label string used as the Calendar header
		* @config MY_LABEL_YEAR_POSITION
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_YEAR_POSITION.key,	{ value:defCfg.MY_LABEL_YEAR_POSITION.value, handler:this.configLocale, validator:this.cfg.checkNumber } );
		
		/**
		* The suffix used after the month when rendering the Calendar header
		* @config MY_LABEL_MONTH_SUFFIX
		* @type String
		* @default " "
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_MONTH_SUFFIX.key,	{ value:defCfg.MY_LABEL_MONTH_SUFFIX.value, handler:this.configLocale } );
		
		/**
		* The suffix used after the year when rendering the Calendar header
		* @config MY_LABEL_YEAR_SUFFIX
		* @type String
		* @default ""
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_YEAR_SUFFIX.key, { value:defCfg.MY_LABEL_YEAR_SUFFIX.value, handler:this.configLocale } );

		/**
		* Configuration for the Month/Year CalendarNavigator UI which allows the user to jump directly to a 
		* specific Month/Year without having to scroll sequentially through months.
		* <p>
		* Setting this property to null (default value) or false, will disable the CalendarNavigator UI.
		* </p>
		* <p>
		* Setting this property to true will enable the CalendarNavigatior UI with the default CalendarNavigator configuration values.
		* </p>
		* <p>
		* This property can also be set to an object literal containing configuration properties for the CalendarNavigator UI.
		* The configuration object expects the the following case-sensitive properties, with the "strings" property being a nested object.
		* Any properties which are not provided will use the default values (defined in the CalendarNavigator class).
		* </p>
		* <dl>
		* <dt>strings</dt>
		* <dd><em>Object</em> :  An object with the properties shown below, defining the string labels to use in the Navigator's UI
		*     <dl>
		*         <dt>month</dt><dd><em>String</em> : The string to use for the month label. Defaults to "Month".</dd>
		*         <dt>year</dt><dd><em>String</em> : The string to use for the year label. Defaults to "Year".</dd>
		*         <dt>submit</dt><dd><em>String</em> : The string to use for the submit button label. Defaults to "Okay".</dd>
		*         <dt>cancel</dt><dd><em>String</em> : The string to use for the cancel button label. Defaults to "Cancel".</dd>
		*         <dt>invalidYear</dt><dd><em>String</em> : The string to use for invalid year values. Defaults to "Year needs to be a number".</dd>
		*     </dl>
		* </dd>
		* <dt>monthFormat</dt><dd><em>String</em> : The month format to use. Either YAHOO.widget.Calendar.LONG, or YAHOO.widget.Calendar.SHORT. Defaults to YAHOO.widget.Calendar.LONG</dd>
		* <dt>initialFocus</dt><dd><em>String</em> : Either "year" or "month" specifying which input control should get initial focus. Defaults to "year"</dd>
		* </dl>
		* <p>E.g.</p>
		* <pre>
		* var navConfig = {
		*	  strings: {
		*		  month:"Calendar Month",
		*		  year:"Calendar Year",
		*		  submit: "Submit",
		*		  cancel: "Cancel",
		*		  invalidYear: "Please enter a valid year"
		*	  },
		*	  monthFormat: YAHOO.widget.Calendar.SHORT,
		*	  initialFocus: "month"
		* }
		* </pre>
		* @config navigator
		* @type {Object|Boolean}
		* @default null
		*/
		this.cfg.addProperty(defCfg.NAV.key, { value:defCfg.NAV.value, handler:this.configNavigator } );
	},

	/**
	* The default handler for the "pagedate" property
	* @method configPageDate
	*/
	configPageDate : function(type, args, obj) {
		this.cfg.setProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key, this._parsePageDate(args[0]), true);
	},

	/**
	* The default handler for the "mindate" property
	* @method configMinDate
	*/
	configMinDate : function(type, args, obj) {
		var val = args[0];
		if (YAHOO.lang.isString(val)) {
			val = this._parseDate(val);
			this.cfg.setProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.MINDATE.key, YAHOO.widget.DateMath.getDate(val[0],(val[1]-1),val[2]));
		}
	},

	/**
	* The default handler for the "maxdate" property
	* @method configMaxDate
	*/
	configMaxDate : function(type, args, obj) {
		var val = args[0];
		if (YAHOO.lang.isString(val)) {
			val = this._parseDate(val);
			this.cfg.setProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.MAXDATE.key, YAHOO.widget.DateMath.getDate(val[0],(val[1]-1),val[2]));
		}
	},
	
	/**
	* The default handler for the "selected" property
	* @method configSelected
	*/
	configSelected : function(type, args, obj) {
		var selected = args[0];
		var cfgSelected = YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key;
		
		if (selected) {
			if (YAHOO.lang.isString(selected)) {
				this.cfg.setProperty(cfgSelected, this._parseDates(selected), true);
			} 
		}
		if (! this._selectedDates) {
			this._selectedDates = this.cfg.getProperty(cfgSelected);
		}
	},
	
	/**
	* The default handler for all configuration options properties
	* @method configOptions
	*/
	configOptions : function(type, args, obj) {
		this.Options[type.toUpperCase()] = args[0];
	},
	
	/**
	* The default handler for all configuration locale properties
	* @method configLocale
	*/
	configLocale : function(type, args, obj) {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
		this.Locale[type.toUpperCase()] = args[0];
	
		this.cfg.refireEvent(defCfg.LOCALE_MONTHS.key);
		this.cfg.refireEvent(defCfg.LOCALE_WEEKDAYS.key);
	},
	
	/**
	* The default handler for all configuration locale field length properties
	* @method configLocaleValues
	*/
	configLocaleValues : function(type, args, obj) {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG; 
	
		type = type.toLowerCase();
		var val = args[0];
	
		switch (type) {
			case defCfg.LOCALE_MONTHS.key:
				switch (val) {
					case YAHOO.widget.Calendar.SHORT:
						this.Locale.LOCALE_MONTHS = this.cfg.getProperty(defCfg.MONTHS_SHORT.key).concat();
						break;
					case YAHOO.widget.Calendar.LONG:
						this.Locale.LOCALE_MONTHS = this.cfg.getProperty(defCfg.MONTHS_LONG.key).concat();
						break;
				}
				break;
			case defCfg.LOCALE_WEEKDAYS.key:
				switch (val) {
					case YAHOO.widget.Calendar.ONE_CHAR:
						this.Locale.LOCALE_WEEKDAYS = this.cfg.getProperty(defCfg.WEEKDAYS_1CHAR.key).concat();
						break;
					case YAHOO.widget.Calendar.SHORT:
						this.Locale.LOCALE_WEEKDAYS = this.cfg.getProperty(defCfg.WEEKDAYS_SHORT.key).concat();
						break;
					case YAHOO.widget.Calendar.MEDIUM:
						this.Locale.LOCALE_WEEKDAYS = this.cfg.getProperty(defCfg.WEEKDAYS_MEDIUM.key).concat();
						break;
					case YAHOO.widget.Calendar.LONG:
						this.Locale.LOCALE_WEEKDAYS = this.cfg.getProperty(defCfg.WEEKDAYS_LONG.key).concat();
						break;
				}
				
				var START_WEEKDAY = this.cfg.getProperty(defCfg.START_WEEKDAY.key);
	
				if (START_WEEKDAY > 0) {
					for (var w=0;w<START_WEEKDAY;++w) {
						this.Locale.LOCALE_WEEKDAYS.push(this.Locale.LOCALE_WEEKDAYS.shift());
					}
				}
				break;
		}
	},

	/**
	 * The default handler for the "navigator" property
	 * @method configNavigator
	 */
	configNavigator : function(type, args, obj) {
		var val = args[0];
		if (YAHOO.widget.CalendarNavigator && (val === true || YAHOO.lang.isObject(val))) {
			if (!this.oNavigator) {
				this.oNavigator = new YAHOO.widget.CalendarNavigator(this);
				// Cleanup DOM Refs/Events before innerHTML is removed.
				function erase() {
					if (!this.pages) {
						this.oNavigator.erase();
					}
				}
				this.beforeRenderEvent.subscribe(erase, this, true);
			}
		} else {
			if (this.oNavigator) {
				this.oNavigator.destroy();
				this.oNavigator = null;
			}
		}
	},

	/**
	* Defines the style constants for the Calendar
	* @method initStyles
	*/
	initStyles : function() {

		var defStyle = YAHOO.widget.Calendar._STYLES;

		this.Style = {
			/**
			* @property Style.CSS_ROW_HEADER
			*/
			CSS_ROW_HEADER: defStyle.CSS_ROW_HEADER,
			/**
			* @property Style.CSS_ROW_FOOTER
			*/
			CSS_ROW_FOOTER: defStyle.CSS_ROW_FOOTER,
			/**
			* @property Style.CSS_CELL
			*/
			CSS_CELL : defStyle.CSS_CELL,
			/**
			* @property Style.CSS_CELL_SELECTOR
			*/
			CSS_CELL_SELECTOR : defStyle.CSS_CELL_SELECTOR,
			/**
			* @property Style.CSS_CELL_SELECTED
			*/
			CSS_CELL_SELECTED : defStyle.CSS_CELL_SELECTED,
			/**
			* @property Style.CSS_CELL_SELECTABLE
			*/
			CSS_CELL_SELECTABLE : defStyle.CSS_CELL_SELECTABLE,
			/**
			* @property Style.CSS_CELL_RESTRICTED
			*/
			CSS_CELL_RESTRICTED : defStyle.CSS_CELL_RESTRICTED,
			/**
			* @property Style.CSS_CELL_TODAY
			*/
			CSS_CELL_TODAY : defStyle.CSS_CELL_TODAY,
			/**
			* @property Style.CSS_CELL_OOM
			*/
			CSS_CELL_OOM : defStyle.CSS_CELL_OOM,
			/**
			* @property Style.CSS_CELL_OOB
			*/
			CSS_CELL_OOB : defStyle.CSS_CELL_OOB,
			/**
			* @property Style.CSS_HEADER
			*/
			CSS_HEADER : defStyle.CSS_HEADER,
			/**
			* @property Style.CSS_HEADER_TEXT
			*/
			CSS_HEADER_TEXT : defStyle.CSS_HEADER_TEXT,
			/**
			* @property Style.CSS_BODY
			*/
			CSS_BODY : defStyle.CSS_BODY,
			/**
			* @property Style.CSS_WEEKDAY_CELL
			*/
			CSS_WEEKDAY_CELL : defStyle.CSS_WEEKDAY_CELL,
			/**
			* @property Style.CSS_WEEKDAY_ROW
			*/
			CSS_WEEKDAY_ROW : defStyle.CSS_WEEKDAY_ROW,
			/**
			* @property Style.CSS_FOOTER
			*/
			CSS_FOOTER : defStyle.CSS_FOOTER,
			/**
			* @property Style.CSS_CALENDAR
			*/
			CSS_CALENDAR : defStyle.CSS_CALENDAR,
			/**
			* @property Style.CSS_SINGLE
			*/
			CSS_SINGLE : defStyle.CSS_SINGLE,
			/**
			* @property Style.CSS_CONTAINER
			*/
			CSS_CONTAINER : defStyle.CSS_CONTAINER,
			/**
			* @property Style.CSS_NAV_LEFT
			*/
			CSS_NAV_LEFT : defStyle.CSS_NAV_LEFT,
			/**
			* @property Style.CSS_NAV_RIGHT
			*/
			CSS_NAV_RIGHT : defStyle.CSS_NAV_RIGHT,
			/**
			* @property Style.CSS_NAV
			*/
			CSS_NAV : defStyle.CSS_NAV,
			/**
			* @property Style.CSS_CLOSE
			*/
			CSS_CLOSE : defStyle.CSS_CLOSE,
			/**
			* @property Style.CSS_CELL_TOP
			*/
			CSS_CELL_TOP : defStyle.CSS_CELL_TOP,
			/**
			* @property Style.CSS_CELL_LEFT
			*/
			CSS_CELL_LEFT : defStyle.CSS_CELL_LEFT,
			/**
			* @property Style.CSS_CELL_RIGHT
			*/
			CSS_CELL_RIGHT : defStyle.CSS_CELL_RIGHT,
			/**
			* @property Style.CSS_CELL_BOTTOM
			*/
			CSS_CELL_BOTTOM : defStyle.CSS_CELL_BOTTOM,
			/**
			* @property Style.CSS_CELL_HOVER
			*/
			CSS_CELL_HOVER : defStyle.CSS_CELL_HOVER,
			/**
			* @property Style.CSS_CELL_HIGHLIGHT1
			*/
			CSS_CELL_HIGHLIGHT1 : defStyle.CSS_CELL_HIGHLIGHT1,
			/**
			* @property Style.CSS_CELL_HIGHLIGHT2
			*/
			CSS_CELL_HIGHLIGHT2 : defStyle.CSS_CELL_HIGHLIGHT2,
			/**
			* @property Style.CSS_CELL_HIGHLIGHT3
			*/
			CSS_CELL_HIGHLIGHT3 : defStyle.CSS_CELL_HIGHLIGHT3,
			/**
			* @property Style.CSS_CELL_HIGHLIGHT4
			*/
			CSS_CELL_HIGHLIGHT4 : defStyle.CSS_CELL_HIGHLIGHT4
		};
	},
	
	/**
	* Builds the date label that will be displayed in the calendar header or
	* footer, depending on configuration.
	* @method buildMonthLabel
	* @return	{String}	The formatted calendar month label
	*/
	buildMonthLabel : function() {
		var pageDate = this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key);
	
		var monthLabel  = this.Locale.LOCALE_MONTHS[pageDate.getMonth()] + this.Locale.MY_LABEL_MONTH_SUFFIX;
		var yearLabel = pageDate.getFullYear() + this.Locale.MY_LABEL_YEAR_SUFFIX;

		if (this.Locale.MY_LABEL_MONTH_POSITION == 2 || this.Locale.MY_LABEL_YEAR_POSITION == 1) {
			return yearLabel + monthLabel;
		} else {
			return monthLabel + yearLabel;
		}
	},
	
	/**
	* Builds the date digit that will be displayed in calendar cells
	* @method buildDayLabel
	* @param {Date}	workingDate	The current working date
	* @return	{String}	The formatted day label
	*/
	buildDayLabel : function(workingDate) {
		return workingDate.getDate();
	},
	
	/**
	 * Creates the title bar element and adds it to Calendar container DIV
	 * 
	 * @method createTitleBar
	 * @param {String} strTitle The title to display in the title bar
	 * @return The title bar element
	 */
	createTitleBar : function(strTitle) {
		var tDiv = YAHOO.util.Dom.getElementsByClassName(YAHOO.widget.CalendarGroup.CSS_2UPTITLE, "div", this.oDomContainer)[0] || document.createElement("div");
		tDiv.className = YAHOO.widget.CalendarGroup.CSS_2UPTITLE;
		tDiv.innerHTML = strTitle;
		this.oDomContainer.insertBefore(tDiv, this.oDomContainer.firstChild);
	
		YAHOO.util.Dom.addClass(this.oDomContainer, "withtitle");
	
		return tDiv;
	},
	
	/**
	 * Removes the title bar element from the DOM
	 * 
	 * @method removeTitleBar
	 */
	removeTitleBar : function() {
		var tDiv = YAHOO.util.Dom.getElementsByClassName(YAHOO.widget.CalendarGroup.CSS_2UPTITLE, "div", this.oDomContainer)[0] || null;
		if (tDiv) {
			YAHOO.util.Event.purgeElement(tDiv);
			this.oDomContainer.removeChild(tDiv);
		}
		YAHOO.util.Dom.removeClass(this.oDomContainer, "withtitle");
	},
	
	/**
	 * Creates the close button HTML element and adds it to Calendar container DIV
	 * 
	 * @method createCloseButton
	 * @return The close HTML element created
	 */
	createCloseButton : function() {
		var Dom = YAHOO.util.Dom,
			Event = YAHOO.util.Event,
			cssClose = YAHOO.widget.CalendarGroup.CSS_2UPCLOSE,
			DEPR_CLOSE_PATH = "us/my/bn/x_d.gif";
	
		var lnk = Dom.getElementsByClassName("link-close", "a", this.oDomContainer)[0];
	
		if (!lnk) {
			lnk = document.createElement("a");  
			Event.addListener(lnk, "click", function(e, cal) {
				cal.hide(); 
				Event.preventDefault(e);
			}, this);        
		}
	
		lnk.href = "#";
		lnk.className = "link-close";
	
		if (YAHOO.widget.Calendar.IMG_ROOT !== null) {
			var img = Dom.getElementsByClassName(cssClose, "img", lnk)[0] || document.createElement("img");
			img.src = YAHOO.widget.Calendar.IMG_ROOT + DEPR_CLOSE_PATH;
			img.className = cssClose;
			lnk.appendChild(img);
		} else {
			lnk.innerHTML = '<span class="' + cssClose + ' ' + this.Style.CSS_CLOSE + '"></span>';
		}
		this.oDomContainer.appendChild(lnk);
	
		return lnk;
	},
	
	/**
	 * Removes the close button HTML element from the DOM
	 * 
	 * @method removeCloseButton
	 */
	removeCloseButton : function() {
		var btn = YAHOO.util.Dom.getElementsByClassName("link-close", "a", this.oDomContainer)[0] || null;
		if (btn) {
			YAHOO.util.Event.purgeElement(btn);
			this.oDomContainer.removeChild(btn);
		}
	},

	/**
	* Renders the calendar header.
	* @method renderHeader
	* @param {Array}	html	The current working HTML array
	* @return {Array} The current working HTML array
	*/
	renderHeader : function(html) {
		var colSpan = 7;
		
		var DEPR_NAV_LEFT = "us/tr/callt.gif";
		var DEPR_NAV_RIGHT = "us/tr/calrt.gif";	
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
		
		if (this.cfg.getProperty(defCfg.SHOW_WEEK_HEADER.key)) {
			colSpan += 1;
		}
	
		if (this.cfg.getProperty(defCfg.SHOW_WEEK_FOOTER.key)) {
			colSpan += 1;
		}
	
		html[html.length] = "<thead>";
		html[html.length] =		"<tr>";
		html[html.length] =			'<th colspan="' + colSpan + '" class="' + this.Style.CSS_HEADER_TEXT + '">';
		html[html.length] =				'<div class="' + this.Style.CSS_HEADER + '">';
	
		var renderLeft, renderRight = false;
	
		if (this.parent) {
			if (this.index === 0) {
				renderLeft = true;
			}
			if (this.index == (this.parent.cfg.getProperty("pages") -1)) {
				renderRight = true;
			}
		} else {
			renderLeft = true;
			renderRight = true;
		}
	
		if (renderLeft) {
			var leftArrow = this.cfg.getProperty(defCfg.NAV_ARROW_LEFT.key);
			// Check for deprecated customization - If someone set IMG_ROOT, but didn't set NAV_ARROW_LEFT, then set NAV_ARROW_LEFT to the old deprecated value
			if (leftArrow === null && YAHOO.widget.Calendar.IMG_ROOT !== null) {
				leftArrow = YAHOO.widget.Calendar.IMG_ROOT + DEPR_NAV_LEFT;
			}
			var leftStyle = (leftArrow === null) ? "" : ' style="background-image:url(' + leftArrow + ')"';
			html[html.length] = '<a class="' + this.Style.CSS_NAV_LEFT + '"' + leftStyle + ' >&#160;</a>';
		}

		var lbl = this.buildMonthLabel();
		var cal = this.parent || this;
		if (cal.cfg.getProperty("navigator")) {
			lbl = "<a class=\"" + this.Style.CSS_NAV + "\" href=\"#\">" + lbl + "</a>";
		}
		html[html.length] = lbl;

		if (renderRight) {
			var rightArrow = this.cfg.getProperty(defCfg.NAV_ARROW_RIGHT.key);
			if (rightArrow === null && YAHOO.widget.Calendar.IMG_ROOT !== null) {
				rightArrow = YAHOO.widget.Calendar.IMG_ROOT + DEPR_NAV_RIGHT;
			}
			var rightStyle = (rightArrow === null) ? "" : ' style="background-image:url(' + rightArrow + ')"';
			html[html.length] = '<a class="' + this.Style.CSS_NAV_RIGHT + '"' + rightStyle + ' >&#160;</a>';
		}

		html[html.length] =	'</div>\n</th>\n</tr>';

		if (this.cfg.getProperty(defCfg.SHOW_WEEKDAYS.key)) {
			html = this.buildWeekdays(html);
		}
		
		html[html.length] = '</thead>';
	
		return html;
	},
	
	/**
	* Renders the Calendar's weekday headers.
	* @method buildWeekdays
	* @param {Array}	html	The current working HTML array
	* @return {Array} The current working HTML array
	*/
	buildWeekdays : function(html) {
	
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
	
		html[html.length] = '<tr class="' + this.Style.CSS_WEEKDAY_ROW + '">';
	
		if (this.cfg.getProperty(defCfg.SHOW_WEEK_HEADER.key)) {
			html[html.length] = '<th>&#160;</th>';
		}
	
		for(var i=0;i<this.Locale.LOCALE_WEEKDAYS.length;++i) {
			html[html.length] = '<th class="calweekdaycell">' + this.Locale.LOCALE_WEEKDAYS[i] + '</th>';
		}
	
		if (this.cfg.getProperty(defCfg.SHOW_WEEK_FOOTER.key)) {
			html[html.length] = '<th>&#160;</th>';
		}
	
		html[html.length] = '</tr>';
	
		return html;
	},
	
	/**
	* Renders the calendar body.
	* @method renderBody
	* @param {Date}	workingDate	The current working Date being used for the render process
	* @param {Array}	html	The current working HTML array
	* @return {Array} The current working HTML array
	*/
	renderBody : function(workingDate, html) {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
	
		var startDay = this.cfg.getProperty(defCfg.START_WEEKDAY.key);
	
		this.preMonthDays = workingDate.getDay();
		if (startDay > 0) {
			this.preMonthDays -= startDay;
		}
		if (this.preMonthDays < 0) {
			this.preMonthDays += 7;
		}
		
		this.monthDays = YAHOO.widget.DateMath.findMonthEnd(workingDate).getDate();
		this.postMonthDays = YAHOO.widget.Calendar.DISPLAY_DAYS-this.preMonthDays-this.monthDays;
		
		workingDate = YAHOO.widget.DateMath.subtract(workingDate, YAHOO.widget.DateMath.DAY, this.preMonthDays);
	
		var weekNum,weekClass;
		var weekPrefix = "w";
		var cellPrefix = "_cell";
		var workingDayPrefix = "wd";
		var dayPrefix = "d";
		
		var cellRenderers;
		var renderer;
		
		var todayYear = this.today.getFullYear();
		var todayMonth = this.today.getMonth();
		var todayDate = this.today.getDate();
		
		var useDate = this.cfg.getProperty(defCfg.PAGEDATE.key);
		var hideBlankWeeks = this.cfg.getProperty(defCfg.HIDE_BLANK_WEEKS.key);
		var showWeekFooter = this.cfg.getProperty(defCfg.SHOW_WEEK_FOOTER.key);
		var showWeekHeader = this.cfg.getProperty(defCfg.SHOW_WEEK_HEADER.key);
		var mindate = this.cfg.getProperty(defCfg.MINDATE.key);
		var maxdate = this.cfg.getProperty(defCfg.MAXDATE.key);
	
		if (mindate) {
			mindate = YAHOO.widget.DateMath.clearTime(mindate);
		}
		if (maxdate) {
			maxdate = YAHOO.widget.DateMath.clearTime(maxdate);
		}
		
		html[html.length] = '<tbody class="m' + (useDate.getMonth()+1) + ' ' + this.Style.CSS_BODY + '">';
		
		var i = 0;
	
		var tempDiv = document.createElement("div");
		var cell = document.createElement("td");
		tempDiv.appendChild(cell);
	
		var cal = this.parent || this;
	
		for (var r=0;r<6;r++) {
	
			weekNum = YAHOO.widget.DateMath.getWeekNumber(workingDate, useDate.getFullYear(), startDay);
			weekClass = weekPrefix + weekNum;
	
			// Local OOM check for performance, since we already have pagedate
			if (r !== 0 && hideBlankWeeks === true && workingDate.getMonth() != useDate.getMonth()) {
				break;
			} else {
	
				html[html.length] = '<tr class="' + weekClass + '">';
				
				if (showWeekHeader) { html = this.renderRowHeader(weekNum, html); }
				
				for (var d=0;d<7;d++){ // Render actual days
	
					cellRenderers = [];
	
					this.clearElement(cell);
					cell.className = this.Style.CSS_CELL;
					cell.id = this.id + cellPrefix + i;

					if (workingDate.getDate()		== todayDate && 
						workingDate.getMonth()		== todayMonth &&
						workingDate.getFullYear()	== todayYear) {
						cellRenderers[cellRenderers.length]=cal.renderCellStyleToday;
					}
					
					var workingArray = [workingDate.getFullYear(),workingDate.getMonth()+1,workingDate.getDate()];
					this.cellDates[this.cellDates.length] = workingArray; // Add this date to cellDates
					
					// Local OOM check for performance, since we already have pagedate
					if (workingDate.getMonth() != useDate.getMonth()) {
						cellRenderers[cellRenderers.length]=cal.renderCellNotThisMonth;
					} else {
						YAHOO.util.Dom.addClass(cell, workingDayPrefix + workingDate.getDay());
						YAHOO.util.Dom.addClass(cell, dayPrefix + workingDate.getDate());
					
						for (var s=0;s<this.renderStack.length;++s) {
	
							renderer = null;
	
							var rArray = this.renderStack[s];
							var type = rArray[0];
							
							var month;
							var day;
							var year;
							
							switch (type) {
								case YAHOO.widget.Calendar.DATE:
									month = rArray[1][1];
									day = rArray[1][2];
									year = rArray[1][0];
	
									if (workingDate.getMonth()+1 == month && workingDate.getDate() == day && workingDate.getFullYear() == year) {
										renderer = rArray[2];
										this.renderStack.splice(s,1);
									}
									break;
								case YAHOO.widget.Calendar.MONTH_DAY:
									month = rArray[1][0];
									day = rArray[1][1];
									
									if (workingDate.getMonth()+1 == month && workingDate.getDate() == day) {
										renderer = rArray[2];
										this.renderStack.splice(s,1);
									}
									break;
								case YAHOO.widget.Calendar.RANGE:
									var date1 = rArray[1][0];
									var date2 = rArray[1][1];
	
									var d1month = date1[1];
									var d1day = date1[2];
									var d1year = date1[0];
									
									var d1 = YAHOO.widget.DateMath.getDate(d1year, d1month-1, d1day);
	
									var d2month = date2[1];
									var d2day = date2[2];
									var d2year = date2[0];
	
									var d2 = YAHOO.widget.DateMath.getDate(d2year, d2month-1, d2day);
	
									if (workingDate.getTime() >= d1.getTime() && workingDate.getTime() <= d2.getTime()) {
										renderer = rArray[2];
	
										if (workingDate.getTime()==d2.getTime()) { 
											this.renderStack.splice(s,1);
										}
									}
									break;
								case YAHOO.widget.Calendar.WEEKDAY:
									
									var weekday = rArray[1][0];
									if (workingDate.getDay()+1 == weekday) {
										renderer = rArray[2];
									}
									break;
								case YAHOO.widget.Calendar.MONTH:
									
									month = rArray[1][0];
									if (workingDate.getMonth()+1 == month) {
										renderer = rArray[2];
									}
									break;
							}
							
							if (renderer) {
								cellRenderers[cellRenderers.length]=renderer;
							}
						}
	
					}
	
					if (this._indexOfSelectedFieldArray(workingArray) > -1) {
						cellRenderers[cellRenderers.length]=cal.renderCellStyleSelected; 
					}
	
					if ((mindate && (workingDate.getTime() < mindate.getTime())) ||
						(maxdate && (workingDate.getTime() > maxdate.getTime()))
					) {
						cellRenderers[cellRenderers.length]=cal.renderOutOfBoundsDate;
					} else {
						cellRenderers[cellRenderers.length]=cal.styleCellDefault;
						cellRenderers[cellRenderers.length]=cal.renderCellDefault;	
					}
					
					for (var x=0; x < cellRenderers.length; ++x) {
						if (cellRenderers[x].call(cal, workingDate, cell) == YAHOO.widget.Calendar.STOP_RENDER) {
							break;
						}
					}
	
					workingDate.setTime(workingDate.getTime() + YAHOO.widget.DateMath.ONE_DAY_MS);
	
					if (i >= 0 && i <= 6) {
						YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_TOP);
					}
					if ((i % 7) === 0) {
						YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_LEFT);
					}
					if (((i+1) % 7) === 0) {
						YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_RIGHT);
					}
					
					var postDays = this.postMonthDays; 
					if (hideBlankWeeks && postDays >= 7) {
						var blankWeeks = Math.floor(postDays/7);
						for (var p=0;p<blankWeeks;++p) {
							postDays -= 7;
						}
					}
					
					if (i >= ((this.preMonthDays+postDays+this.monthDays)-7)) {
						YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_BOTTOM);
					}
	
					html[html.length] = tempDiv.innerHTML;
					i++;
				}
	
				if (showWeekFooter) { html = this.renderRowFooter(weekNum, html); }
	
				html[html.length] = '</tr>';
			}
		}
	
		html[html.length] = '</tbody>';
	
		return html;
	},
	
	/**
	* Renders the calendar footer. In the default implementation, there is
	* no footer.
	* @method renderFooter
	* @param {Array}	html	The current working HTML array
	* @return {Array} The current working HTML array
	*/
	renderFooter : function(html) { return html; },
	
	/**
	* Renders the calendar after it has been configured. The render() method has a specific call chain that will execute
	* when the method is called: renderHeader, renderBody, renderFooter.
	* Refer to the documentation for those methods for information on 
	* individual render tasks.
	* @method render
	*/
	render : function() {
		this.beforeRenderEvent.fire();
	
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
	
		// Find starting day of the current month
		var workingDate = YAHOO.widget.DateMath.findMonthStart(this.cfg.getProperty(defCfg.PAGEDATE.key));
	
		this.resetRenderers();
		this.cellDates.length = 0;

		YAHOO.util.Event.purgeElement(this.oDomContainer, true);

		var html = [];
	
		html[html.length] = '<table cellSpacing="0" class="' + this.Style.CSS_CALENDAR + ' y' + workingDate.getFullYear() + '" id="' + this.id + '">';
		html = this.renderHeader(html);
		html = this.renderBody(workingDate, html);
		html = this.renderFooter(html);
		html[html.length] = '</table>';

		this.oDomContainer.innerHTML = html.join("\n");

		this.applyListeners();
		this.cells = this.oDomContainer.getElementsByTagName("td");
	
		this.cfg.refireEvent(defCfg.TITLE.key);
		this.cfg.refireEvent(defCfg.CLOSE.key);
		this.cfg.refireEvent(defCfg.IFRAME.key);
	
		this.renderEvent.fire();
	},

	/**
	* Applies the Calendar's DOM listeners to applicable elements.
	* @method applyListeners
	*/
	applyListeners : function() {
		var root = this.oDomContainer;
		var cal = this.parent || this;
		var anchor = "a";
		var mousedown = "mousedown";

		var linkLeft = YAHOO.util.Dom.getElementsByClassName(this.Style.CSS_NAV_LEFT, anchor, root);
		var linkRight = YAHOO.util.Dom.getElementsByClassName(this.Style.CSS_NAV_RIGHT, anchor, root);
	
		if (linkLeft && linkLeft.length > 0) {
			this.linkLeft = linkLeft[0];
			YAHOO.util.Event.addListener(this.linkLeft, mousedown, cal.previousMonth, cal, true);
		}

		if (linkRight && linkRight.length > 0) {
			this.linkRight = linkRight[0];
			YAHOO.util.Event.addListener(this.linkRight, mousedown, cal.nextMonth, cal, true);
		}

		if (cal.cfg.getProperty("navigator") !== null) {
			this.applyNavListeners();
		}

		if (this.domEventMap) {
			var el,elements;
			for (var cls in this.domEventMap) {	
				if (YAHOO.lang.hasOwnProperty(this.domEventMap, cls)) {
					var items = this.domEventMap[cls];
	
					if (! (items instanceof Array)) {
						items = [items];
					}
	
					for (var i=0;i<items.length;i++)	{
						var item = items[i];
						elements = YAHOO.util.Dom.getElementsByClassName(cls, item.tag, this.oDomContainer);
	
						for (var c=0;c<elements.length;c++) {
							el = elements[c];
							 YAHOO.util.Event.addListener(el, item.event, item.handler, item.scope, item.correct );
						}
					}
				}
			}
		}
	
		YAHOO.util.Event.addListener(this.oDomContainer, "click", this.doSelectCell, this);
		YAHOO.util.Event.addListener(this.oDomContainer, "mouseover", this.doCellMouseOver, this);
		YAHOO.util.Event.addListener(this.oDomContainer, "mouseout", this.doCellMouseOut, this);
	},

	applyNavListeners : function() {

		var E = YAHOO.util.Event;

		var calParent = this.parent || this;
		var cal = this;

		var navBtns = YAHOO.util.Dom.getElementsByClassName(this.Style.CSS_NAV, "a", this.oDomContainer);

		if (navBtns.length > 0) {

			function show(e, obj) {
				var target = E.getTarget(e);
				// this == navBtn
				if (this === target || YAHOO.util.Dom.isAncestor(this, target)) {
					E.preventDefault(e);
				}
				var navigator = calParent.oNavigator;
				if (navigator) {
					var pgdate = cal.cfg.getProperty("pagedate");
					navigator.setYear(pgdate.getFullYear());
					navigator.setMonth(pgdate.getMonth());
					navigator.show();
				}
			}
			E.addListener(navBtns, "click", show);
		}
	},

	/**
	* Retrieves the Date object for the specified Calendar cell
	* @method getDateByCellId
	* @param {String}	id	The id of the cell
	* @return {Date} The Date object for the specified Calendar cell
	*/
	getDateByCellId : function(id) {
		var date = this.getDateFieldsByCellId(id);
		return YAHOO.widget.DateMath.getDate(date[0],date[1]-1,date[2]);
	},
	
	/**
	* Retrieves the Date object for the specified Calendar cell
	* @method getDateFieldsByCellId
	* @param {String}	id	The id of the cell
	* @return {Array}	The array of Date fields for the specified Calendar cell
	*/
	getDateFieldsByCellId : function(id) {
		id = id.toLowerCase().split("_cell")[1];
		id = parseInt(id, 10);
		return this.cellDates[id];
	},
	
	/**
	 * Find the Calendar's cell index for a given date.
	 * If the date is not found, the method returns -1.
	 * <p>
	 * The returned index can be used to lookup the cell HTMLElement  
	 * using the Calendar's cells array or passed to selectCell to select 
	 * cells by index. 
	 * </p>
	 *
	 * See <a href="#cells">cells</a>, <a href="#selectCell">selectCell</a>.
	 *
	 * @method getCellIndex
	 * @param {Date} date JavaScript Date object, for which to find a cell index.
	 * @return {Number} The index of the date in Calendars cellDates/cells arrays, or -1 if the date 
	 * is not on the curently rendered Calendar page.
	 */
	getCellIndex : function(date) {
		var idx = -1;
		if (date) {
			var m = date.getMonth(),
				y = date.getFullYear(),
				d = date.getDate(),
				dates = this.cellDates;

			for (var i = 0; i < dates.length; ++i) {
				var cellDate = dates[i];
				if (cellDate[0] === y && cellDate[1] === m+1 && cellDate[2] === d) {
					idx = i;
					break;
				}
			}
		}
		return idx;
	},
	
	// BEGIN BUILT-IN TABLE CELL RENDERERS
	
	/**
	* Renders a cell that falls before the minimum date or after the maximum date.
	* widget class.
	* @method renderOutOfBoundsDate
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	* @return {String} YAHOO.widget.Calendar.STOP_RENDER if rendering should stop with this style, null or nothing if rendering
	*			should not be terminated
	*/
	renderOutOfBoundsDate : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_OOB);
		cell.innerHTML = workingDate.getDate();
		return YAHOO.widget.Calendar.STOP_RENDER;
	},
	
	/**
	* Renders the row header for a week.
	* @method renderRowHeader
	* @param {Number}	weekNum	The week number of the current row
	* @param {Array}	cell	The current working HTML array
	*/
	renderRowHeader : function(weekNum, html) {
		html[html.length] = '<th class="calrowhead">' + weekNum + '</th>';
		return html;
	},
	
	/**
	* Renders the row footer for a week.
	* @method renderRowFooter
	* @param {Number}	weekNum	The week number of the current row
	* @param {Array}	cell	The current working HTML array
	*/
	renderRowFooter : function(weekNum, html) {
		html[html.length] = '<th class="calrowfoot">' + weekNum + '</th>';
		return html;
	},
	
	/**
	* Renders a single standard calendar cell in the calendar widget table.
	* All logic for determining how a standard default cell will be rendered is 
	* encapsulated in this method, and must be accounted for when extending the
	* widget class.
	* @method renderCellDefault
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellDefault : function(workingDate, cell) {
		cell.innerHTML = '<a href="#" class="' + this.Style.CSS_CELL_SELECTOR + '">' + this.buildDayLabel(workingDate) + "</a>";
	},
	
	/**
	* Styles a selectable cell.
	* @method styleCellDefault
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	styleCellDefault : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_SELECTABLE);
	},
	
	
	/**
	* Renders a single standard calendar cell using the CSS hightlight1 style
	* @method renderCellStyleHighlight1
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellStyleHighlight1 : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_HIGHLIGHT1);
	},
	
	/**
	* Renders a single standard calendar cell using the CSS hightlight2 style
	* @method renderCellStyleHighlight2
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellStyleHighlight2 : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_HIGHLIGHT2);
	},
	
	/**
	* Renders a single standard calendar cell using the CSS hightlight3 style
	* @method renderCellStyleHighlight3
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellStyleHighlight3 : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_HIGHLIGHT3);
	},
	
	/**
	* Renders a single standard calendar cell using the CSS hightlight4 style
	* @method renderCellStyleHighlight4
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellStyleHighlight4 : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_HIGHLIGHT4);
	},
	
	/**
	* Applies the default style used for rendering today's date to the current calendar cell
	* @method renderCellStyleToday
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	*/
	renderCellStyleToday : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_TODAY);
	},
	
	/**
	* Applies the default style used for rendering selected dates to the current calendar cell
	* @method renderCellStyleSelected
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	* @return {String} YAHOO.widget.Calendar.STOP_RENDER if rendering should stop with this style, null or nothing if rendering
	*			should not be terminated
	*/
	renderCellStyleSelected : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_SELECTED);
	},
	
	/**
	* Applies the default style used for rendering dates that are not a part of the current
	* month (preceding or trailing the cells for the current month)
	* @method renderCellNotThisMonth
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	* @return {String} YAHOO.widget.Calendar.STOP_RENDER if rendering should stop with this style, null or nothing if rendering
	*			should not be terminated
	*/
	renderCellNotThisMonth : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_OOM);
		cell.innerHTML=workingDate.getDate();
		return YAHOO.widget.Calendar.STOP_RENDER;
	},
	
	/**
	* Renders the current calendar cell as a non-selectable "black-out" date using the default
	* restricted style.
	* @method renderBodyCellRestricted
	* @param {Date}					workingDate		The current working Date object being used to generate the calendar
	* @param {HTMLTableCellElement}	cell			The current working cell in the calendar
	* @return {String} YAHOO.widget.Calendar.STOP_RENDER if rendering should stop with this style, null or nothing if rendering
	*			should not be terminated
	*/
	renderBodyCellRestricted : function(workingDate, cell) {
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL);
		YAHOO.util.Dom.addClass(cell, this.Style.CSS_CELL_RESTRICTED);
		cell.innerHTML=workingDate.getDate();
		return YAHOO.widget.Calendar.STOP_RENDER;
	},
	
	// END BUILT-IN TABLE CELL RENDERERS
	
	// BEGIN MONTH NAVIGATION METHODS
	
	/**
	* Adds the designated number of months to the current calendar month, and sets the current
	* calendar page date to the new month.
	* @method addMonths
	* @param {Number}	count	The number of months to add to the current calendar
	*/
	addMonths : function(count) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		this.cfg.setProperty(cfgPageDate, YAHOO.widget.DateMath.add(this.cfg.getProperty(cfgPageDate), YAHOO.widget.DateMath.MONTH, count));
		this.resetRenderers();
		this.changePageEvent.fire();
	},
	
	/**
	* Subtracts the designated number of months from the current calendar month, and sets the current
	* calendar page date to the new month.
	* @method subtractMonths
	* @param {Number}	count	The number of months to subtract from the current calendar
	*/
	subtractMonths : function(count) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		this.cfg.setProperty(cfgPageDate, YAHOO.widget.DateMath.subtract(this.cfg.getProperty(cfgPageDate), YAHOO.widget.DateMath.MONTH, count));
		this.resetRenderers();
		this.changePageEvent.fire();
	},
	
	/**
	* Adds the designated number of years to the current calendar, and sets the current
	* calendar page date to the new month.
	* @method addYears
	* @param {Number}	count	The number of years to add to the current calendar
	*/
	addYears : function(count) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		this.cfg.setProperty(cfgPageDate, YAHOO.widget.DateMath.add(this.cfg.getProperty(cfgPageDate), YAHOO.widget.DateMath.YEAR, count));
		this.resetRenderers();
		this.changePageEvent.fire();
	},
	
	/**
	* Subtcats the designated number of years from the current calendar, and sets the current
	* calendar page date to the new month.
	* @method subtractYears
	* @param {Number}	count	The number of years to subtract from the current calendar
	*/
	subtractYears : function(count) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		this.cfg.setProperty(cfgPageDate, YAHOO.widget.DateMath.subtract(this.cfg.getProperty(cfgPageDate), YAHOO.widget.DateMath.YEAR, count));
		this.resetRenderers();
		this.changePageEvent.fire();
	},
	
	/**
	* Navigates to the next month page in the calendar widget.
	* @method nextMonth
	*/
	nextMonth : function() {
		this.addMonths(1);
	},
	
	/**
	* Navigates to the previous month page in the calendar widget.
	* @method previousMonth
	*/
	previousMonth : function() {
		this.subtractMonths(1);
	},
	
	/**
	* Navigates to the next year in the currently selected month in the calendar widget.
	* @method nextYear
	*/
	nextYear : function() {
		this.addYears(1);
	},
	
	/**
	* Navigates to the previous year in the currently selected month in the calendar widget.
	* @method previousYear
	*/
	previousYear : function() {
		this.subtractYears(1);
	},
	
	// END MONTH NAVIGATION METHODS
	
	// BEGIN SELECTION METHODS
	
	/**
	* Resets the calendar widget to the originally selected month and year, and 
	* sets the calendar to the initial selection(s).
	* @method reset
	*/
	reset : function() {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
		this.cfg.resetProperty(defCfg.SELECTED.key);
		this.cfg.resetProperty(defCfg.PAGEDATE.key);
		this.resetEvent.fire();
	},
	
	/**
	* Clears the selected dates in the current calendar widget and sets the calendar
	* to the current month and year.
	* @method clear
	*/
	clear : function() {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
		this.cfg.setProperty(defCfg.SELECTED.key, []);
		this.cfg.setProperty(defCfg.PAGEDATE.key, new Date(this.today.getTime()));
		this.clearEvent.fire();
	},
	
	/**
	* Selects a date or a collection of dates on the current calendar. This method, by default,
	* does not call the render method explicitly. Once selection has completed, render must be 
	* called for the changes to be reflected visually.
	*
	* Any dates which are OOB (out of bounds, not selectable) will not be selected and the array of 
	* selected dates passed to the selectEvent will not contain OOB dates.
	* 
	* If all dates are OOB, the no state change will occur; beforeSelect and select events will not be fired.
	*
	* @method select
	* @param	{String/Date/Date[]}	date	The date string of dates to select in the current calendar. Valid formats are
	*								individual date(s) (12/24/2005,12/26/2005) or date range(s) (12/24/2005-1/1/2006).
	*								Multiple comma-delimited dates can also be passed to this method (12/24/2005,12/11/2005-12/13/2005).
	*								This method can also take a JavaScript Date object or an array of Date objects.
	* @return	{Date[]}			Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	select : function(date) {
	
		var aToBeSelected = this._toFieldArray(date);
	
		// Filtered array of valid dates
		var validDates = [];
		var selected = [];
		var cfgSelected = YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key;
		
		for (var a=0; a < aToBeSelected.length; ++a) {
			var toSelect = aToBeSelected[a];
	
			if (!this.isDateOOB(this._toDate(toSelect))) {
				
				if (validDates.length === 0) {
					this.beforeSelectEvent.fire();
					selected = this.cfg.getProperty(cfgSelected);
				}
	
				validDates.push(toSelect);
				
				if (this._indexOfSelectedFieldArray(toSelect) == -1) { 
					selected[selected.length] = toSelect;
				}
			}
		}
		
	
		if (validDates.length > 0) {
			if (this.parent) {
				this.parent.cfg.setProperty(cfgSelected, selected);
			} else {
				this.cfg.setProperty(cfgSelected, selected);
			}
			this.selectEvent.fire(validDates);
		}
	
		return this.getSelectedDates();
	},
	
	/**
	* Selects a date on the current calendar by referencing the index of the cell that should be selected.
	* This method is used to easily select a single cell (usually with a mouse click) without having to do
	* a full render. The selected style is applied to the cell directly.
	*
	* If the cell is not marked with the CSS_CELL_SELECTABLE class (as is the case by default for out of month 
	* or out of bounds cells), it will not be selected and in such a case beforeSelect and select events will not be fired.
	* 
	* @method selectCell
	* @param	{Number}	cellIndex	The index of the cell to select in the current calendar. 
	* @return	{Date[]}	Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	selectCell : function(cellIndex) {
	
		var cell = this.cells[cellIndex];
		var cellDate = this.cellDates[cellIndex];
		var dCellDate = this._toDate(cellDate);
		
		var selectable = YAHOO.util.Dom.hasClass(cell, this.Style.CSS_CELL_SELECTABLE);
	
		if (selectable) {
	
			this.beforeSelectEvent.fire();
	
			var cfgSelected = YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key;
			var selected = this.cfg.getProperty(cfgSelected);
	
			var selectDate = cellDate.concat();
	
			if (this._indexOfSelectedFieldArray(selectDate) == -1) {
				selected[selected.length] = selectDate;
			}
			if (this.parent) {
				this.parent.cfg.setProperty(cfgSelected, selected);
			} else {
				this.cfg.setProperty(cfgSelected, selected);
			}
			this.renderCellStyleSelected(dCellDate,cell);
			this.selectEvent.fire([selectDate]);
	
			this.doCellMouseOut.call(cell, null, this);		
		}
	
		return this.getSelectedDates();
	},
	
	/**
	* Deselects a date or a collection of dates on the current calendar. This method, by default,
	* does not call the render method explicitly. Once deselection has completed, render must be 
	* called for the changes to be reflected visually.
	* 
	* The method will not attempt to deselect any dates which are OOB (out of bounds, and hence not selectable) 
	* and the array of deselected dates passed to the deselectEvent will not contain any OOB dates.
	* 
	* If all dates are OOB, beforeDeselect and deselect events will not be fired.
	* 
	* @method deselect
	* @param	{String/Date/Date[]}	date	The date string of dates to deselect in the current calendar. Valid formats are
	*								individual date(s) (12/24/2005,12/26/2005) or date range(s) (12/24/2005-1/1/2006).
	*								Multiple comma-delimited dates can also be passed to this method (12/24/2005,12/11/2005-12/13/2005).
	*								This method can also take a JavaScript Date object or an array of Date objects.	
	* @return	{Date[]}			Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	deselect : function(date) {
	
		var aToBeDeselected = this._toFieldArray(date);
	
		var validDates = [];
		var selected = [];
		var cfgSelected = YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key;
	
		for (var a=0; a < aToBeDeselected.length; ++a) {
			var toDeselect = aToBeDeselected[a];
	
			if (!this.isDateOOB(this._toDate(toDeselect))) {
	
				if (validDates.length === 0) {
					this.beforeDeselectEvent.fire();
					selected = this.cfg.getProperty(cfgSelected);
				}
	
				validDates.push(toDeselect);
	
				var index = this._indexOfSelectedFieldArray(toDeselect);
				if (index != -1) {	
					selected.splice(index,1);
				}
			}
		}
	
	
		if (validDates.length > 0) {
			if (this.parent) {
				this.parent.cfg.setProperty(cfgSelected, selected);
			} else {
				this.cfg.setProperty(cfgSelected, selected);
			}
			this.deselectEvent.fire(validDates);
		}
	
		return this.getSelectedDates();
	},
	
	/**
	* Deselects a date on the current calendar by referencing the index of the cell that should be deselected.
	* This method is used to easily deselect a single cell (usually with a mouse click) without having to do
	* a full render. The selected style is removed from the cell directly.
	* 
	* If the cell is not marked with the CSS_CELL_SELECTABLE class (as is the case by default for out of month 
	* or out of bounds cells), the method will not attempt to deselect it and in such a case, beforeDeselect and 
	* deselect events will not be fired.
	* 
	* @method deselectCell
	* @param	{Number}	cellIndex	The index of the cell to deselect in the current calendar. 
	* @return	{Date[]}	Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	deselectCell : function(cellIndex) {
		var cell = this.cells[cellIndex];
		var cellDate = this.cellDates[cellIndex];
		var cellDateIndex = this._indexOfSelectedFieldArray(cellDate);
		
		var selectable = YAHOO.util.Dom.hasClass(cell, this.Style.CSS_CELL_SELECTABLE);
	
		if (selectable) {
	
			this.beforeDeselectEvent.fire();
	
			var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
			var selected = this.cfg.getProperty(defCfg.SELECTED.key);
	
			var dCellDate = this._toDate(cellDate);
			var selectDate = cellDate.concat();
	
			if (cellDateIndex > -1) {
				if (this.cfg.getProperty(defCfg.PAGEDATE.key).getMonth() == dCellDate.getMonth() &&
					this.cfg.getProperty(defCfg.PAGEDATE.key).getFullYear() == dCellDate.getFullYear()) {
					YAHOO.util.Dom.removeClass(cell, this.Style.CSS_CELL_SELECTED);
				}
				selected.splice(cellDateIndex, 1);
			}
	
			if (this.parent) {
				this.parent.cfg.setProperty(defCfg.SELECTED.key, selected);
			} else {
				this.cfg.setProperty(defCfg.SELECTED.key, selected);
			}
	
			this.deselectEvent.fire(selectDate);
		}
	
		return this.getSelectedDates();
	},
	
	/**
	* Deselects all dates on the current calendar.
	* @method deselectAll
	* @return {Date[]}		Array of JavaScript Date objects representing all individual dates that are currently selected.
	*						Assuming that this function executes properly, the return value should be an empty array.
	*						However, the empty array is returned for the sake of being able to check the selection status
	*						of the calendar.
	*/
	deselectAll : function() {
		this.beforeDeselectEvent.fire();
		
		var cfgSelected = YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key;
	
		var selected = this.cfg.getProperty(cfgSelected);
		var count = selected.length;
		var sel = selected.concat();
	
		if (this.parent) {
			this.parent.cfg.setProperty(cfgSelected, []);
		} else {
			this.cfg.setProperty(cfgSelected, []);
		}
		
		if (count > 0) {
			this.deselectEvent.fire(sel);
		}
	
		return this.getSelectedDates();
	},
	
	// END SELECTION METHODS
	
	// BEGIN TYPE CONVERSION METHODS
	
	/**
	* Converts a date (either a JavaScript Date object, or a date string) to the internal data structure
	* used to represent dates: [[yyyy,mm,dd],[yyyy,mm,dd]].
	* @method _toFieldArray
	* @private
	* @param	{String/Date/Date[]}	date	The date string of dates to deselect in the current calendar. Valid formats are
	*								individual date(s) (12/24/2005,12/26/2005) or date range(s) (12/24/2005-1/1/2006).
	*								Multiple comma-delimited dates can also be passed to this method (12/24/2005,12/11/2005-12/13/2005).
	*								This method can also take a JavaScript Date object or an array of Date objects.	
	* @return {Array[](Number[])}	Array of date field arrays
	*/
	_toFieldArray : function(date) {
		var returnDate = [];
	
		if (date instanceof Date) {
			returnDate = [[date.getFullYear(), date.getMonth()+1, date.getDate()]];
		} else if (YAHOO.lang.isString(date)) {
			returnDate = this._parseDates(date);
		} else if (YAHOO.lang.isArray(date)) {
			for (var i=0;i<date.length;++i) {
				var d = date[i];
				returnDate[returnDate.length] = [d.getFullYear(),d.getMonth()+1,d.getDate()];
			}
		}
		
		return returnDate;
	},
	
	/**
	* Converts a date field array [yyyy,mm,dd] to a JavaScript Date object. The date field array
	* is the format in which dates are as provided as arguments to selectEvent and deselectEvent listeners.
	* 
	* @method toDate
	* @param	{Number[]}	dateFieldArray	The date field array to convert to a JavaScript Date.
	* @return	{Date}	JavaScript Date object representing the date field array.
	*/
	toDate : function(dateFieldArray) {
		return this._toDate(dateFieldArray);
	},
	
	/**
	* Converts a date field array [yyyy,mm,dd] to a JavaScript Date object.
	* @method _toDate
	* @private
	* @deprecated Made public, toDate 
	* @param	{Number[]}		dateFieldArray	The date field array to convert to a JavaScript Date.
	* @return	{Date}	JavaScript Date object representing the date field array
	*/
	_toDate : function(dateFieldArray) {
		if (dateFieldArray instanceof Date) {
			return dateFieldArray;
		} else {
			return YAHOO.widget.DateMath.getDate(dateFieldArray[0],dateFieldArray[1]-1,dateFieldArray[2]);
		}
	},
	
	// END TYPE CONVERSION METHODS 
	
	// BEGIN UTILITY METHODS
	
	/**
	* Converts a date field array [yyyy,mm,dd] to a JavaScript Date object.
	* @method _fieldArraysAreEqual
	* @private
	* @param	{Number[]}	array1	The first date field array to compare
	* @param	{Number[]}	array2	The first date field array to compare
	* @return	{Boolean}	The boolean that represents the equality of the two arrays
	*/
	_fieldArraysAreEqual : function(array1, array2) {
		var match = false;
	
		if (array1[0]==array2[0]&&array1[1]==array2[1]&&array1[2]==array2[2]) {
			match=true;	
		}
	
		return match;
	},
	
	/**
	* Gets the index of a date field array [yyyy,mm,dd] in the current list of selected dates.
	* @method	_indexOfSelectedFieldArray
	* @private
	* @param	{Number[]}		find	The date field array to search for
	* @return	{Number}			The index of the date field array within the collection of selected dates.
	*								-1 will be returned if the date is not found.
	*/
	_indexOfSelectedFieldArray : function(find) {
		var selected = -1;
		var seldates = this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key);
	
		for (var s=0;s<seldates.length;++s) {
			var sArray = seldates[s];
			if (find[0]==sArray[0]&&find[1]==sArray[1]&&find[2]==sArray[2]) {
				selected = s;
				break;
			}
		}
	
		return selected;
	},
	
	/**
	* Determines whether a given date is OOM (out of month).
	* @method	isDateOOM
	* @param	{Date}	date	The JavaScript Date object for which to check the OOM status
	* @return	{Boolean}	true if the date is OOM
	*/
	isDateOOM : function(date) {
		return (date.getMonth() != this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key).getMonth());
	},
	
	/**
	* Determines whether a given date is OOB (out of bounds - less than the mindate or more than the maxdate).
	*
	* @method	isDateOOB
	* @param	{Date}	date	The JavaScript Date object for which to check the OOB status
	* @return	{Boolean}	true if the date is OOB
	*/
	isDateOOB : function(date) {
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
		
		var minDate = this.cfg.getProperty(defCfg.MINDATE.key);
		var maxDate = this.cfg.getProperty(defCfg.MAXDATE.key);
		var dm = YAHOO.widget.DateMath;
		
		if (minDate) {
			minDate = dm.clearTime(minDate);
		} 
		if (maxDate) {
			maxDate = dm.clearTime(maxDate);
		}
	
		var clearedDate = new Date(date.getTime());
		clearedDate = dm.clearTime(clearedDate);
	
		return ((minDate && clearedDate.getTime() < minDate.getTime()) || (maxDate && clearedDate.getTime() > maxDate.getTime()));
	},
	
	/**
	 * Parses a pagedate configuration property value. The value can either be specified as a string of form "mm/yyyy" or a Date object 
	 * and is parsed into a Date object normalized to the first day of the month. If no value is passed in, the month and year from today's date are used to create the Date object 
	 * @method	_parsePageDate
	 * @private
	 * @param {Date|String}	date	Pagedate value which needs to be parsed
	 * @return {Date}	The Date object representing the pagedate
	 */
	_parsePageDate : function(date) {
		var parsedDate;
		
		var defCfg = YAHOO.widget.Calendar._DEFAULT_CONFIG;
	
		if (date) {
			if (date instanceof Date) {
				parsedDate = YAHOO.widget.DateMath.findMonthStart(date);
			} else {
				var month, year, aMonthYear;
				aMonthYear = date.split(this.cfg.getProperty(defCfg.DATE_FIELD_DELIMITER.key));
				month = parseInt(aMonthYear[this.cfg.getProperty(defCfg.MY_MONTH_POSITION.key)-1], 10)-1;
				year = parseInt(aMonthYear[this.cfg.getProperty(defCfg.MY_YEAR_POSITION.key)-1], 10);

				parsedDate = YAHOO.widget.DateMath.getDate(year, month, 1);
			}
		} else {
			parsedDate = YAHOO.widget.DateMath.getDate(this.today.getFullYear(), this.today.getMonth(), 1);
		}
		return parsedDate;
	},
	
	// END UTILITY METHODS
	
	// BEGIN EVENT HANDLERS
	
	/**
	* Event executed before a date is selected in the calendar widget.
	* @deprecated Event handlers for this event should be susbcribed to beforeSelectEvent.
	*/
	onBeforeSelect : function() {
		if (this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.MULTI_SELECT.key) === false) {
			if (this.parent) {
				this.parent.callChildFunction("clearAllBodyCellStyles", this.Style.CSS_CELL_SELECTED);
				this.parent.deselectAll();
			} else {
				this.clearAllBodyCellStyles(this.Style.CSS_CELL_SELECTED);
				this.deselectAll();
			}
		}
	},
	
	/**
	* Event executed when a date is selected in the calendar widget.
	* @param	{Array}	selected	An array of date field arrays representing which date or dates were selected. Example: [ [2006,8,6],[2006,8,7],[2006,8,8] ]
	* @deprecated Event handlers for this event should be susbcribed to selectEvent.
	*/
	onSelect : function(selected) { },
	
	/**
	* Event executed before a date is deselected in the calendar widget.
	* @deprecated Event handlers for this event should be susbcribed to beforeDeselectEvent.
	*/
	onBeforeDeselect : function() { },
	
	/**
	* Event executed when a date is deselected in the calendar widget.
	* @param	{Array}	selected	An array of date field arrays representing which date or dates were deselected. Example: [ [2006,8,6],[2006,8,7],[2006,8,8] ]
	* @deprecated Event handlers for this event should be susbcribed to deselectEvent.
	*/
	onDeselect : function(deselected) { },
	
	/**
	* Event executed when the user navigates to a different calendar page.
	* @deprecated Event handlers for this event should be susbcribed to changePageEvent.
	*/
	onChangePage : function() {
		this.render();
	},
	
	/**
	* Event executed when the calendar widget is rendered.
	* @deprecated Event handlers for this event should be susbcribed to renderEvent.
	*/
	onRender : function() { },
	
	/**
	* Event executed when the calendar widget is reset to its original state.
	* @deprecated Event handlers for this event should be susbcribed to resetEvemt.
	*/
	onReset : function() { this.render(); },
	
	/**
	* Event executed when the calendar widget is completely cleared to the current month with no selections.
	* @deprecated Event handlers for this event should be susbcribed to clearEvent.
	*/
	onClear : function() { this.render(); },
	
	/**
	* Validates the calendar widget. This method has no default implementation
	* and must be extended by subclassing the widget.
	* @return	Should return true if the widget validates, and false if
	* it doesn't.
	* @type Boolean
	*/
	validate : function() { return true; },
	
	// END EVENT HANDLERS
	
	// BEGIN DATE PARSE METHODS
	
	/**
	* Converts a date string to a date field array
	* @private
	* @param	{String}	sDate			Date string. Valid formats are mm/dd and mm/dd/yyyy.
	* @return				A date field array representing the string passed to the method
	* @type Array[](Number[])
	*/
	_parseDate : function(sDate) {
		var aDate = sDate.split(this.Locale.DATE_FIELD_DELIMITER);
		var rArray;
	
		if (aDate.length == 2) {
			rArray = [aDate[this.Locale.MD_MONTH_POSITION-1],aDate[this.Locale.MD_DAY_POSITION-1]];
			rArray.type = YAHOO.widget.Calendar.MONTH_DAY;
		} else {
			rArray = [aDate[this.Locale.MDY_YEAR_POSITION-1],aDate[this.Locale.MDY_MONTH_POSITION-1],aDate[this.Locale.MDY_DAY_POSITION-1]];
			rArray.type = YAHOO.widget.Calendar.DATE;
		}
	
		for (var i=0;i<rArray.length;i++) {
			rArray[i] = parseInt(rArray[i], 10);
		}
	
		return rArray;
	},
	
	/**
	* Converts a multi or single-date string to an array of date field arrays
	* @private
	* @param	{String}	sDates		Date string with one or more comma-delimited dates. Valid formats are mm/dd, mm/dd/yyyy, mm/dd/yyyy-mm/dd/yyyy
	* @return							An array of date field arrays
	* @type Array[](Number[])
	*/
	_parseDates : function(sDates) {
		var aReturn = [];
	
		var aDates = sDates.split(this.Locale.DATE_DELIMITER);
		
		for (var d=0;d<aDates.length;++d) {
			var sDate = aDates[d];
	
			if (sDate.indexOf(this.Locale.DATE_RANGE_DELIMITER) != -1) {
				// This is a range
				var aRange = sDate.split(this.Locale.DATE_RANGE_DELIMITER);
	
				var dateStart = this._parseDate(aRange[0]);
				var dateEnd = this._parseDate(aRange[1]);
	
				var fullRange = this._parseRange(dateStart, dateEnd);
				aReturn = aReturn.concat(fullRange);
			} else {
				// This is not a range
				var aDate = this._parseDate(sDate);
				aReturn.push(aDate);
			}
		}
		return aReturn;
	},
	
	/**
	* Converts a date range to the full list of included dates
	* @private
	* @param	{Number[]}	startDate	Date field array representing the first date in the range
	* @param	{Number[]}	endDate		Date field array representing the last date in the range
	* @return							An array of date field arrays
	* @type Array[](Number[])
	*/
	_parseRange : function(startDate, endDate) {
		var dCurrent = YAHOO.widget.DateMath.add(YAHOO.widget.DateMath.getDate(startDate[0],startDate[1]-1,startDate[2]),YAHOO.widget.DateMath.DAY,1);
		var dEnd     = YAHOO.widget.DateMath.getDate(endDate[0],  endDate[1]-1,  endDate[2]);
	
		var results = [];
		results.push(startDate);
		while (dCurrent.getTime() <= dEnd.getTime()) {
			results.push([dCurrent.getFullYear(),dCurrent.getMonth()+1,dCurrent.getDate()]);
			dCurrent = YAHOO.widget.DateMath.add(dCurrent,YAHOO.widget.DateMath.DAY,1);
		}
		return results;
	},
	
	// END DATE PARSE METHODS
	
	// BEGIN RENDERER METHODS
	
	/**
	* Resets the render stack of the current calendar to its original pre-render value.
	*/
	resetRenderers : function() {
		this.renderStack = this._renderStack.concat();
	},
	
	/**
	 * Removes all custom renderers added to the Calendar through the addRenderer, addMonthRenderer and 
	 * addWeekdayRenderer methods. Calendar's render method needs to be called after removing renderers 
	 * to re-render the Calendar without custom renderers applied.
	 */
	removeRenderers : function() {
		this._renderStack = [];
		this.renderStack = [];
	},

	/**
	* Clears the inner HTML, CSS class and style information from the specified cell.
	* @method clearElement
	* @param	{HTMLTableCellElement} cell The cell to clear
	*/ 
	clearElement : function(cell) {
		cell.innerHTML = "&#160;";
		cell.className="";
	},
	
	/**
	* Adds a renderer to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the conditions specified in the date string for this renderer.
	* @method addRenderer
	* @param	{String}	sDates		A date string to associate with the specified renderer. Valid formats
	*									include date (12/24/2005), month/day (12/24), and range (12/1/2004-1/1/2005)
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addRenderer : function(sDates, fnRender) {
		var aDates = this._parseDates(sDates);
		for (var i=0;i<aDates.length;++i) {
			var aDate = aDates[i];
		
			if (aDate.length == 2) { // this is either a range or a month/day combo
				if (aDate[0] instanceof Array) { // this is a range
					this._addRenderer(YAHOO.widget.Calendar.RANGE,aDate,fnRender);
				} else { // this is a month/day combo
					this._addRenderer(YAHOO.widget.Calendar.MONTH_DAY,aDate,fnRender);
				}
			} else if (aDate.length == 3) {
				this._addRenderer(YAHOO.widget.Calendar.DATE,aDate,fnRender);
			}
		}
	},
	
	/**
	* The private method used for adding cell renderers to the local render stack.
	* This method is called by other methods that set the renderer type prior to the method call.
	* @method _addRenderer
	* @private
	* @param	{String}	type		The type string that indicates the type of date renderer being added.
	*									Values are YAHOO.widget.Calendar.DATE, YAHOO.widget.Calendar.MONTH_DAY, YAHOO.widget.Calendar.WEEKDAY,
	*									YAHOO.widget.Calendar.RANGE, YAHOO.widget.Calendar.MONTH
	* @param	{Array}		aDates		An array of dates used to construct the renderer. The format varies based
	*									on the renderer type
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	_addRenderer : function(type, aDates, fnRender) {
		var add = [type,aDates,fnRender];
		this.renderStack.unshift(add);	
		this._renderStack = this.renderStack.concat();
	},

	/**
	* Adds a month to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the month passed to this method.
	* @method addMonthRenderer
	* @param	{Number}	month		The month (1-12) to associate with this renderer
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addMonthRenderer : function(month, fnRender) {
		this._addRenderer(YAHOO.widget.Calendar.MONTH,[month],fnRender);
	},

	/**
	* Adds a weekday to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the weekday passed to this method.
	* @method addWeekdayRenderer
	* @param	{Number}	weekday		The weekday (0-6) to associate with this renderer
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addWeekdayRenderer : function(weekday, fnRender) {
		this._addRenderer(YAHOO.widget.Calendar.WEEKDAY,[weekday],fnRender);
	},
	
	// END RENDERER METHODS
	
	// BEGIN CSS METHODS
	
	/**
	* Removes all styles from all body cells in the current calendar table.
	* @method clearAllBodyCellStyles
	* @param	{style}	style The CSS class name to remove from all calendar body cells
	*/
	clearAllBodyCellStyles : function(style) {
		for (var c=0;c<this.cells.length;++c) {
			YAHOO.util.Dom.removeClass(this.cells[c],style);
		}
	},
	
	// END CSS METHODS
	
	// BEGIN GETTER/SETTER METHODS
	/**
	* Sets the calendar's month explicitly
	* @method setMonth
	* @param {Number}	month		The numeric month, from 0 (January) to 11 (December)
	*/
	setMonth : function(month) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		var current = this.cfg.getProperty(cfgPageDate);
		current.setMonth(parseInt(month, 10));
		this.cfg.setProperty(cfgPageDate, current);
	},

	/**
	* Sets the calendar's year explicitly.
	* @method setYear
	* @param {Number}	year		The numeric 4-digit year
	*/
	setYear : function(year) {
		var cfgPageDate = YAHOO.widget.Calendar._DEFAULT_CONFIG.PAGEDATE.key;
		var current = this.cfg.getProperty(cfgPageDate);
		current.setFullYear(parseInt(year, 10));
		this.cfg.setProperty(cfgPageDate, current);
	},

	/**
	* Gets the list of currently selected dates from the calendar.
	* @method getSelectedDates
	* @return {Date[]} An array of currently selected JavaScript Date objects.
	*/
	getSelectedDates : function() {
		var returnDates = [];
		var selected = this.cfg.getProperty(YAHOO.widget.Calendar._DEFAULT_CONFIG.SELECTED.key);

		for (var d=0;d<selected.length;++d) {
			var dateArray = selected[d];

			var date = YAHOO.widget.DateMath.getDate(dateArray[0],dateArray[1]-1,dateArray[2]);
			returnDates.push(date);
		}

		returnDates.sort( function(a,b) { return a-b; } );
		return returnDates;
	},

	/// END GETTER/SETTER METHODS ///
	
	/**
	* Hides the Calendar's outer container from view.
	* @method hide
	*/
	hide : function() {
		if (this.beforeHideEvent.fire()) {
			this.oDomContainer.style.display = "none";
			this.hideEvent.fire();
		}
	},

	/**
	* Shows the Calendar's outer container.
	* @method show
	*/
	show : function() {
		if (this.beforeShowEvent.fire()) {
			this.oDomContainer.style.display = "block";
			this.showEvent.fire();
		}
	},

	/**
	* Returns a string representing the current browser.
	* @deprecated As of 2.3.0, environment information is available in YAHOO.env.ua
	* @see YAHOO.env.ua
	* @property browser
	* @type String
	*/
	browser : (function() {
				var ua = navigator.userAgent.toLowerCase();
					  if (ua.indexOf('opera')!=-1) { // Opera (check first in case of spoof)
						 return 'opera';
					  } else if (ua.indexOf('msie 7')!=-1) { // IE7
						 return 'ie7';
					  } else if (ua.indexOf('msie') !=-1) { // IE
						 return 'ie';
					  } else if (ua.indexOf('safari')!=-1) { // Safari (check before Gecko because it includes "like Gecko")
						 return 'safari';
					  } else if (ua.indexOf('gecko') != -1) { // Gecko
						 return 'gecko';
					  } else {
						 return false;
					  }
				})(),
	/**
	* Returns a string representation of the object.
	* @method toString
	* @return {String}	A string representation of the Calendar object.
	*/
	toString : function() {
		return "Calendar " + this.id;
	}
};

/**
* @namespace YAHOO.widget
* @class Calendar_Core
* @extends YAHOO.widget.Calendar
* @deprecated The old Calendar_Core class is no longer necessary.
*/
YAHOO.widget.Calendar_Core = YAHOO.widget.Calendar;

YAHOO.widget.Cal_Core = YAHOO.widget.Calendar;

/**
* YAHOO.widget.CalendarGroup is a special container class for YAHOO.widget.Calendar. This class facilitates
* the ability to have multi-page calendar views that share a single dataset and are
* dependent on each other.
*
* The calendar group instance will refer to each of its elements using a 0-based index.
* For example, to construct the placeholder for a calendar group widget with id "cal1" and
* containerId of "cal1Container", the markup would be as follows:
*	<xmp>
*		<div id="cal1Container_0"></div>
*		<div id="cal1Container_1"></div>
*	</xmp>
* The tables for the calendars ("cal1_0" and "cal1_1") will be inserted into those containers.
* 
* <p>
* <strong>NOTE: As of 2.4.0, the constructor's ID argument is optional.</strong>
* The CalendarGroup can be constructed by simply providing a container ID string, 
* or a reference to a container DIV HTMLElement (the element needs to exist 
* in the document).
* 
* E.g.:
*	<xmp>
*		var c = new YAHOO.widget.CalendarGroup("calContainer", configOptions);
*	</xmp>
* or:
*   <xmp>
*       var containerDiv = YAHOO.util.Dom.get("calContainer");
*		var c = new YAHOO.widget.CalendarGroup(containerDiv, configOptions);
*	</xmp>
* </p>
* <p>
* If not provided, the ID will be generated from the container DIV ID by adding an "_t" suffix.
* For example if an ID is not provided, and the container's ID is "calContainer", the CalendarGroup's ID will be set to "calContainer_t".
* </p>
* 
* @namespace YAHOO.widget
* @class CalendarGroup
* @constructor
* @param {String} id optional The id of the table element that will represent the CalendarGroup widget. As of 2.4.0, this argument is optional.
* @param {String | HTMLElement} container The id of the container div element that will wrap the CalendarGroup table, or a reference to a DIV element which exists in the document.
* @param {Object} config optional The configuration object containing the initial configuration values for the CalendarGroup.
*/
YAHOO.widget.CalendarGroup = function(id, containerId, config) {
	if (arguments.length > 0) {
		this.init.apply(this, arguments);
	}
};

YAHOO.widget.CalendarGroup.prototype = {

	/**
	* Initializes the calendar group. All subclasses must call this method in order for the
	* group to be initialized properly.
	* @method init
	* @param {String} id optional The id of the table element that will represent the CalendarGroup widget. As of 2.4.0, this argument is optional.
	* @param {String | HTMLElement} container The id of the container div element that will wrap the CalendarGroup table, or a reference to a DIV element which exists in the document.
	* @param {Object} config optional The configuration object containing the initial configuration values for the CalendarGroup.
	*/
	init : function(id, container, config) {

		// Normalize 2.4.0, pre 2.4.0 args
		var nArgs = this._parseArgs(arguments);

		id = nArgs.id;
		container = nArgs.container;
		config = nArgs.config;

		this.oDomContainer = YAHOO.util.Dom.get(container);

		if (!this.oDomContainer.id) {
			this.oDomContainer.id = YAHOO.util.Dom.generateId();
		}
		if (!id) {
			id = this.oDomContainer.id + "_t";
		}

		/**
		* The unique id associated with the CalendarGroup
		* @property id
		* @type String
		*/
		this.id = id;

		/**
		* The unique id associated with the CalendarGroup container
		* @property containerId
		* @type String
		*/
		this.containerId = this.oDomContainer.id;

		this.initEvents();
		this.initStyles();

		/**
		* The collection of Calendar pages contained within the CalendarGroup
		* @property pages
		* @type YAHOO.widget.Calendar[]
		*/
		this.pages = [];

		YAHOO.util.Dom.addClass(this.oDomContainer, YAHOO.widget.CalendarGroup.CSS_CONTAINER);
		YAHOO.util.Dom.addClass(this.oDomContainer, YAHOO.widget.CalendarGroup.CSS_MULTI_UP);

		/**
		* The Config object used to hold the configuration variables for the CalendarGroup
		* @property cfg
		* @type YAHOO.util.Config
		*/
		this.cfg = new YAHOO.util.Config(this);

		/**
		* The local object which contains the CalendarGroup's options
		* @property Options
		* @type Object
		*/
		this.Options = {};

		/**
		* The local object which contains the CalendarGroup's locale settings
		* @property Locale
		* @type Object
		*/
		this.Locale = {};

		this.setupConfig();

		if (config) {
			this.cfg.applyConfig(config, true);
		}

		this.cfg.fireQueue();

		// OPERA HACK FOR MISWRAPPED FLOATS
		if (YAHOO.env.ua.opera){
			this.renderEvent.subscribe(this._fixWidth, this, true);
			this.showEvent.subscribe(this._fixWidth, this, true);
		}

	},

	setupConfig : function() {

		var defCfg = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG;

		/**
		* The number of pages to include in the CalendarGroup. This value can only be set once, in the CalendarGroup's constructor arguments.
		* @config pages
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.PAGES.key, { value:defCfg.PAGES.value, validator:this.cfg.checkNumber, handler:this.configPages } );

		/**
		* The month/year representing the current visible Calendar date (mm/yyyy)
		* @config pagedate
		* @type String
		* @default today's date
		*/
		this.cfg.addProperty(defCfg.PAGEDATE.key, { value:new Date(), handler:this.configPageDate } );

		/**
		* The date or range of dates representing the current Calendar selection
		* @config selected
		* @type String
		* @default []
		*/
		this.cfg.addProperty(defCfg.SELECTED.key, { value:[], handler:this.configSelected } );

		/**
		* The title to display above the CalendarGroup's month header
		* @config title
		* @type String
		* @default ""
		*/
		this.cfg.addProperty(defCfg.TITLE.key, { value:defCfg.TITLE.value, handler:this.configTitle } );

		/**
		* Whether or not a close button should be displayed for this CalendarGroup
		* @config close
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.CLOSE.key, { value:defCfg.CLOSE.value, handler:this.configClose } );

		/**
		* Whether or not an iframe shim should be placed under the Calendar to prevent select boxes from bleeding through in Internet Explorer 6 and below.
		* This property is enabled by default for IE6 and below. It is disabled by default for other browsers for performance reasons, but can be 
		* enabled if required.
		* 
		* @config iframe
		* @type Boolean
		* @default true for IE6 and below, false for all other browsers
		*/
		this.cfg.addProperty(defCfg.IFRAME.key, { value:defCfg.IFRAME.value, handler:this.configIframe, validator:this.cfg.checkBoolean } );
	
		/**
		* The minimum selectable date in the current Calendar (mm/dd/yyyy)
		* @config mindate
		* @type String
		* @default null
		*/
		this.cfg.addProperty(defCfg.MINDATE.key, { value:defCfg.MINDATE.value, handler:this.delegateConfig } );
	
		/**
		* The maximum selectable date in the current Calendar (mm/dd/yyyy)
		* @config maxdate
		* @type String
		* @default null
		*/	
		this.cfg.addProperty(defCfg.MAXDATE.key, { value:defCfg.MAXDATE.value, handler:this.delegateConfig  } );
	
		// Options properties
	
		/**
		* True if the Calendar should allow multiple selections. False by default.
		* @config MULTI_SELECT
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.MULTI_SELECT.key,	{ value:defCfg.MULTI_SELECT.value, handler:this.delegateConfig, validator:this.cfg.checkBoolean } );
	
		/**
		* The weekday the week begins on. Default is 0 (Sunday).
		* @config START_WEEKDAY
		* @type number
		* @default 0
		*/	
		this.cfg.addProperty(defCfg.START_WEEKDAY.key,	{ value:defCfg.START_WEEKDAY.value, handler:this.delegateConfig, validator:this.cfg.checkNumber  } );
		
		/**
		* True if the Calendar should show weekday labels. True by default.
		* @config SHOW_WEEKDAYS
		* @type Boolean
		* @default true
		*/	
		this.cfg.addProperty(defCfg.SHOW_WEEKDAYS.key,	{ value:defCfg.SHOW_WEEKDAYS.value, handler:this.delegateConfig, validator:this.cfg.checkBoolean } );
		
		/**
		* True if the Calendar should show week row headers. False by default.
		* @config SHOW_WEEK_HEADER
		* @type Boolean
		* @default false
		*/	
		this.cfg.addProperty(defCfg.SHOW_WEEK_HEADER.key,{ value:defCfg.SHOW_WEEK_HEADER.value, handler:this.delegateConfig, validator:this.cfg.checkBoolean } );
		
		/**
		* True if the Calendar should show week row footers. False by default.
		* @config SHOW_WEEK_FOOTER
		* @type Boolean
		* @default false
		*/
		this.cfg.addProperty(defCfg.SHOW_WEEK_FOOTER.key,{ value:defCfg.SHOW_WEEK_FOOTER.value, handler:this.delegateConfig, validator:this.cfg.checkBoolean } );
		
		/**
		* True if the Calendar should suppress weeks that are not a part of the current month. False by default.
		* @config HIDE_BLANK_WEEKS
		* @type Boolean
		* @default false
		*/		
		this.cfg.addProperty(defCfg.HIDE_BLANK_WEEKS.key,{ value:defCfg.HIDE_BLANK_WEEKS.value, handler:this.delegateConfig, validator:this.cfg.checkBoolean } );
		
		/**
		* The image that should be used for the left navigation arrow.
		* @config NAV_ARROW_LEFT
		* @type String
		* @deprecated	You can customize the image by overriding the default CSS class for the left arrow - "calnavleft"
		* @default null
		*/		
		this.cfg.addProperty(defCfg.NAV_ARROW_LEFT.key,	{ value:defCfg.NAV_ARROW_LEFT.value, handler:this.delegateConfig } );
		
		/**
		* The image that should be used for the right navigation arrow.
		* @config NAV_ARROW_RIGHT
		* @type String
		* @deprecated	You can customize the image by overriding the default CSS class for the right arrow - "calnavright"
		* @default null
		*/		
		this.cfg.addProperty(defCfg.NAV_ARROW_RIGHT.key,	{ value:defCfg.NAV_ARROW_RIGHT.value, handler:this.delegateConfig } );
	
		// Locale properties
		
		/**
		* The short month labels for the current locale.
		* @config MONTHS_SHORT
		* @type String[]
		* @default ["Jan", "Feb", "Mar", "Apr", "May", "Jun", "Jul", "Aug", "Sep", "Oct", "Nov", "Dec"]
		*/
		this.cfg.addProperty(defCfg.MONTHS_SHORT.key,	{ value:defCfg.MONTHS_SHORT.value, handler:this.delegateConfig } );
		
		/**
		* The long month labels for the current locale.
		* @config MONTHS_LONG
		* @type String[]
		* @default ["January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December"
		*/		
		this.cfg.addProperty(defCfg.MONTHS_LONG.key,		{ value:defCfg.MONTHS_LONG.value, handler:this.delegateConfig } );
		
		/**
		* The 1-character weekday labels for the current locale.
		* @config WEEKDAYS_1CHAR
		* @type String[]
		* @default ["S", "M", "T", "W", "T", "F", "S"]
		*/		
		this.cfg.addProperty(defCfg.WEEKDAYS_1CHAR.key,	{ value:defCfg.WEEKDAYS_1CHAR.value, handler:this.delegateConfig } );
		
		/**
		* The short weekday labels for the current locale.
		* @config WEEKDAYS_SHORT
		* @type String[]
		* @default ["Su", "Mo", "Tu", "We", "Th", "Fr", "Sa"]
		*/		
		this.cfg.addProperty(defCfg.WEEKDAYS_SHORT.key,	{ value:defCfg.WEEKDAYS_SHORT.value, handler:this.delegateConfig } );
		
		/**
		* The medium weekday labels for the current locale.
		* @config WEEKDAYS_MEDIUM
		* @type String[]
		* @default ["Sun", "Mon", "Tue", "Wed", "Thu", "Fri", "Sat"]
		*/		
		this.cfg.addProperty(defCfg.WEEKDAYS_MEDIUM.key,	{ value:defCfg.WEEKDAYS_MEDIUM.value, handler:this.delegateConfig } );
		
		/**
		* The long weekday labels for the current locale.
		* @config WEEKDAYS_LONG
		* @type String[]
		* @default ["Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"]
		*/		
		this.cfg.addProperty(defCfg.WEEKDAYS_LONG.key,	{ value:defCfg.WEEKDAYS_LONG.value, handler:this.delegateConfig } );
	
		/**
		* The setting that determines which length of month labels should be used. Possible values are "short" and "long".
		* @config LOCALE_MONTHS
		* @type String
		* @default "long"
		*/
		this.cfg.addProperty(defCfg.LOCALE_MONTHS.key,	{ value:defCfg.LOCALE_MONTHS.value, handler:this.delegateConfig } );
	
		/**
		* The setting that determines which length of weekday labels should be used. Possible values are "1char", "short", "medium", and "long".
		* @config LOCALE_WEEKDAYS
		* @type String
		* @default "short"
		*/	
		this.cfg.addProperty(defCfg.LOCALE_WEEKDAYS.key,	{ value:defCfg.LOCALE_WEEKDAYS.value, handler:this.delegateConfig } );
	
		/**
		* The value used to delimit individual dates in a date string passed to various Calendar functions.
		* @config DATE_DELIMITER
		* @type String
		* @default ","
		*/
		this.cfg.addProperty(defCfg.DATE_DELIMITER.key,		{ value:defCfg.DATE_DELIMITER.value, handler:this.delegateConfig } );
	
		/**
		* The value used to delimit date fields in a date string passed to various Calendar functions.
		* @config DATE_FIELD_DELIMITER
		* @type String
		* @default "/"
		*/	
		this.cfg.addProperty(defCfg.DATE_FIELD_DELIMITER.key,{ value:defCfg.DATE_FIELD_DELIMITER.value, handler:this.delegateConfig } );
	
		/**
		* The value used to delimit date ranges in a date string passed to various Calendar functions.
		* @config DATE_RANGE_DELIMITER
		* @type String
		* @default "-"
		*/
		this.cfg.addProperty(defCfg.DATE_RANGE_DELIMITER.key,{ value:defCfg.DATE_RANGE_DELIMITER.value, handler:this.delegateConfig } );
	
		/**
		* The position of the month in a month/year date string
		* @config MY_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MY_MONTH_POSITION.key,	{ value:defCfg.MY_MONTH_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the year in a month/year date string
		* @config MY_YEAR_POSITION
		* @type Number
		* @default 2
		*/	
		this.cfg.addProperty(defCfg.MY_YEAR_POSITION.key,	{ value:defCfg.MY_YEAR_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the month in a month/day date string
		* @config MD_MONTH_POSITION
		* @type Number
		* @default 1
		*/	
		this.cfg.addProperty(defCfg.MD_MONTH_POSITION.key,	{ value:defCfg.MD_MONTH_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the day in a month/year date string
		* @config MD_DAY_POSITION
		* @type Number
		* @default 2
		*/	
		this.cfg.addProperty(defCfg.MD_DAY_POSITION.key,		{ value:defCfg.MD_DAY_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the month in a month/day/year date string
		* @config MDY_MONTH_POSITION
		* @type Number
		* @default 1
		*/	
		this.cfg.addProperty(defCfg.MDY_MONTH_POSITION.key,	{ value:defCfg.MDY_MONTH_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the day in a month/day/year date string
		* @config MDY_DAY_POSITION
		* @type Number
		* @default 2
		*/	
		this.cfg.addProperty(defCfg.MDY_DAY_POSITION.key,	{ value:defCfg.MDY_DAY_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The position of the year in a month/day/year date string
		* @config MDY_YEAR_POSITION
		* @type Number
		* @default 3
		*/	
		this.cfg.addProperty(defCfg.MDY_YEAR_POSITION.key,	{ value:defCfg.MDY_YEAR_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the month in the month year label string used as the Calendar header
		* @config MY_LABEL_MONTH_POSITION
		* @type Number
		* @default 1
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_MONTH_POSITION.key,	{ value:defCfg.MY_LABEL_MONTH_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
	
		/**
		* The position of the year in the month year label string used as the Calendar header
		* @config MY_LABEL_YEAR_POSITION
		* @type Number
		* @default 2
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_YEAR_POSITION.key,	{ value:defCfg.MY_LABEL_YEAR_POSITION.value, handler:this.delegateConfig, validator:this.cfg.checkNumber } );
		
		/**
		* The suffix used after the month when rendering the Calendar header
		* @config MY_LABEL_MONTH_SUFFIX
		* @type String
		* @default " "
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_MONTH_SUFFIX.key,	{ value:defCfg.MY_LABEL_MONTH_SUFFIX.value, handler:this.delegateConfig } );
		
		/**
		* The suffix used after the year when rendering the Calendar header
		* @config MY_LABEL_YEAR_SUFFIX
		* @type String
		* @default ""
		*/
		this.cfg.addProperty(defCfg.MY_LABEL_YEAR_SUFFIX.key, { value:defCfg.MY_LABEL_YEAR_SUFFIX.value, handler:this.delegateConfig } );

		/**
		* Configuration for the Month Year Navigation UI. By default it is disabled
		* @config NAV
		* @type Object
		* @default null
		*/
		this.cfg.addProperty(defCfg.NAV.key, { value:defCfg.NAV.value, handler:this.configNavigator } );
	},

	/**
	* Initializes CalendarGroup's built-in CustomEvents
	* @method initEvents
	*/
	initEvents : function() {
		var me = this;
		var strEvent = "Event";

		/**
		* Proxy subscriber to subscribe to the CalendarGroup's child Calendars' CustomEvents
		* @method sub
		* @private
		* @param {Function} fn	The function to subscribe to this CustomEvent
		* @param {Object}	obj	The CustomEvent's scope object
		* @param {Boolean}	bOverride	Whether or not to apply scope correction
		*/
		var sub = function(fn, obj, bOverride) {
			for (var p=0;p<me.pages.length;++p) {
				var cal = me.pages[p];
				cal[this.type + strEvent].subscribe(fn, obj, bOverride);
			}
		};

		/**
		* Proxy unsubscriber to unsubscribe from the CalendarGroup's child Calendars' CustomEvents
		* @method unsub
		* @private
		* @param {Function} fn	The function to subscribe to this CustomEvent
		* @param {Object}	obj	The CustomEvent's scope object
		*/
		var unsub = function(fn, obj) {
			for (var p=0;p<me.pages.length;++p) {
				var cal = me.pages[p];
				cal[this.type + strEvent].unsubscribe(fn, obj);
			}
		};
		
		var defEvents = YAHOO.widget.Calendar._EVENT_TYPES;
	
		/**
		* Fired before a selection is made
		* @event beforeSelectEvent
		*/
		this.beforeSelectEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SELECT);
		this.beforeSelectEvent.subscribe = sub; this.beforeSelectEvent.unsubscribe = unsub;
	
		/**
		* Fired when a selection is made
		* @event selectEvent
		* @param {Array}	Array of Date field arrays in the format [YYYY, MM, DD].
		*/
		this.selectEvent = new YAHOO.util.CustomEvent(defEvents.SELECT); 
		this.selectEvent.subscribe = sub; this.selectEvent.unsubscribe = unsub;
	
		/**
		* Fired before a selection is made
		* @event beforeDeselectEvent
		*/
		this.beforeDeselectEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_DESELECT); 
		this.beforeDeselectEvent.subscribe = sub; this.beforeDeselectEvent.unsubscribe = unsub;
	
		/**
		* Fired when a selection is made
		* @event deselectEvent
		* @param {Array}	Array of Date field arrays in the format [YYYY, MM, DD].
		*/
		this.deselectEvent = new YAHOO.util.CustomEvent(defEvents.DESELECT); 
		this.deselectEvent.subscribe = sub; this.deselectEvent.unsubscribe = unsub;
		
		/**
		* Fired when the Calendar page is changed
		* @event changePageEvent
		*/
		this.changePageEvent = new YAHOO.util.CustomEvent(defEvents.CHANGE_PAGE); 
		this.changePageEvent.subscribe = sub; this.changePageEvent.unsubscribe = unsub;
	
		/**
		* Fired before the Calendar is rendered
		* @event beforeRenderEvent
		*/
		this.beforeRenderEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_RENDER);
		this.beforeRenderEvent.subscribe = sub; this.beforeRenderEvent.unsubscribe = unsub;
	
		/**
		* Fired when the Calendar is rendered
		* @event renderEvent
		*/
		this.renderEvent = new YAHOO.util.CustomEvent(defEvents.RENDER);
		this.renderEvent.subscribe = sub; this.renderEvent.unsubscribe = unsub;
	
		/**
		* Fired when the Calendar is reset
		* @event resetEvent
		*/
		this.resetEvent = new YAHOO.util.CustomEvent(defEvents.RESET); 
		this.resetEvent.subscribe = sub; this.resetEvent.unsubscribe = unsub;
	
		/**
		* Fired when the Calendar is cleared
		* @event clearEvent
		*/
		this.clearEvent = new YAHOO.util.CustomEvent(defEvents.CLEAR);
		this.clearEvent.subscribe = sub; this.clearEvent.unsubscribe = unsub;
	
		/**
		* Fired just before the CalendarGroup is to be shown
		* @event beforeShowEvent
		*/
		this.beforeShowEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SHOW);
	
		/**
		* Fired after the CalendarGroup is shown
		* @event showEvent
		*/
		this.showEvent = new YAHOO.util.CustomEvent(defEvents.SHOW);
	
		/**
		* Fired just before the CalendarGroup is to be hidden
		* @event beforeHideEvent
		*/
		this.beforeHideEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_HIDE);
	
		/**
		* Fired after the CalendarGroup is hidden
		* @event hideEvent
		*/
		this.hideEvent = new YAHOO.util.CustomEvent(defEvents.HIDE);

		/**
		* Fired just before the CalendarNavigator is to be shown
		* @event beforeShowNavEvent
		*/
		this.beforeShowNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_SHOW_NAV);
	
		/**
		* Fired after the CalendarNavigator is shown
		* @event showNavEvent
		*/
		this.showNavEvent = new YAHOO.util.CustomEvent(defEvents.SHOW_NAV);
	
		/**
		* Fired just before the CalendarNavigator is to be hidden
		* @event beforeHideNavEvent
		*/
		this.beforeHideNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_HIDE_NAV);
	
		/**
		* Fired after the CalendarNavigator is hidden
		* @event hideNavEvent
		*/
		this.hideNavEvent = new YAHOO.util.CustomEvent(defEvents.HIDE_NAV);

		/**
		* Fired just before the CalendarNavigator is to be rendered
		* @event beforeRenderNavEvent
		*/
		this.beforeRenderNavEvent = new YAHOO.util.CustomEvent(defEvents.BEFORE_RENDER_NAV);

		/**
		* Fired after the CalendarNavigator is rendered
		* @event renderNavEvent
		*/
		this.renderNavEvent = new YAHOO.util.CustomEvent(defEvents.RENDER_NAV);
	},
	
	/**
	* The default Config handler for the "pages" property
	* @method configPages
	* @param {String} type	The CustomEvent type (usually the property name)
	* @param {Object[]}	args	The CustomEvent arguments. For configuration handlers, args[0] will equal the newly applied value for the property.
	* @param {Object} obj	The scope object. For configuration handlers, this will usually equal the owner.
	*/
	configPages : function(type, args, obj) {
		var pageCount = args[0];
	
		var cfgPageDate = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGEDATE.key;
	
		// Define literals outside loop	
		var sep = "_";
		var groupCalClass = "groupcal";
	
		var firstClass = "first-of-type";
		var lastClass = "last-of-type";
	
		for (var p=0;p<pageCount;++p) {
			var calId = this.id + sep + p;
			var calContainerId = this.containerId + sep + p;
	
			var childConfig = this.cfg.getConfig();
			childConfig.close = false;
			childConfig.title = false;
			childConfig.navigator = null;

			var cal = this.constructChild(calId, calContainerId, childConfig);
			var caldate = cal.cfg.getProperty(cfgPageDate);
			this._setMonthOnDate(caldate, caldate.getMonth() + p);
			cal.cfg.setProperty(cfgPageDate, caldate);
	
			YAHOO.util.Dom.removeClass(cal.oDomContainer, this.Style.CSS_SINGLE);
			YAHOO.util.Dom.addClass(cal.oDomContainer, groupCalClass);

			if (p===0) {
				YAHOO.util.Dom.addClass(cal.oDomContainer, firstClass);
			}
	
			if (p==(pageCount-1)) {
				YAHOO.util.Dom.addClass(cal.oDomContainer, lastClass);
			}
	
			cal.parent = this;
			cal.index = p; 
	
			this.pages[this.pages.length] = cal;
		}
	},
	
	/**
	* The default Config handler for the "pagedate" property
	* @method configPageDate
	* @param {String} type	The CustomEvent type (usually the property name)
	* @param {Object[]}	args	The CustomEvent arguments. For configuration handlers, args[0] will equal the newly applied value for the property.
	* @param {Object} obj	The scope object. For configuration handlers, this will usually equal the owner.
	*/
	configPageDate : function(type, args, obj) {
		var val = args[0];
		var firstPageDate;
		
		var cfgPageDate = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGEDATE.key;
		
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			if (p === 0) {
				firstPageDate = cal._parsePageDate(val);
				cal.cfg.setProperty(cfgPageDate, firstPageDate);
			} else {
				var pageDate = new Date(firstPageDate);
				this._setMonthOnDate(pageDate, pageDate.getMonth() + p);
				cal.cfg.setProperty(cfgPageDate, pageDate);
			}
		}
	},
	
	/**
	* The default Config handler for the CalendarGroup "selected" property
	* @method configSelected
	* @param {String} type	The CustomEvent type (usually the property name)
	* @param {Object[]}	args	The CustomEvent arguments. For configuration handlers, args[0] will equal the newly applied value for the property.
	* @param {Object} obj	The scope object. For configuration handlers, this will usually equal the owner.
	*/
	configSelected : function(type, args, obj) {
		var cfgSelected = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.SELECTED.key;
		this.delegateConfig(type, args, obj);
		var selected = (this.pages.length > 0) ? this.pages[0].cfg.getProperty(cfgSelected) : []; 
		this.cfg.setProperty(cfgSelected, selected, true);
	},

	
	/**
	* Delegates a configuration property to the CustomEvents associated with the CalendarGroup's children
	* @method delegateConfig
	* @param {String} type	The CustomEvent type (usually the property name)
	* @param {Object[]}	args	The CustomEvent arguments. For configuration handlers, args[0] will equal the newly applied value for the property.
	* @param {Object} obj	The scope object. For configuration handlers, this will usually equal the owner.
	*/
	delegateConfig : function(type, args, obj) {
		var val = args[0];
		var cal;
	
		for (var p=0;p<this.pages.length;p++) {
			cal = this.pages[p];
			cal.cfg.setProperty(type, val);
		}
	},

	/**
	* Adds a function to all child Calendars within this CalendarGroup.
	* @method setChildFunction
	* @param {String}		fnName		The name of the function
	* @param {Function}		fn			The function to apply to each Calendar page object
	*/
	setChildFunction : function(fnName, fn) {
		var pageCount = this.cfg.getProperty(YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGES.key);
	
		for (var p=0;p<pageCount;++p) {
			this.pages[p][fnName] = fn;
		}
	},

	/**
	* Calls a function within all child Calendars within this CalendarGroup.
	* @method callChildFunction
	* @param {String}		fnName		The name of the function
	* @param {Array}		args		The arguments to pass to the function
	*/
	callChildFunction : function(fnName, args) {
		var pageCount = this.cfg.getProperty(YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGES.key);
	
		for (var p=0;p<pageCount;++p) {
			var page = this.pages[p];
			if (page[fnName]) {
				var fn = page[fnName];
				fn.call(page, args);
			}
		}	
	},

	/**
	* Constructs a child calendar. This method can be overridden if a subclassed version of the default
	* calendar is to be used.
	* @method constructChild
	* @param {String}	id			The id of the table element that will represent the calendar widget
	* @param {String}	containerId	The id of the container div element that will wrap the calendar table
	* @param {Object}	config		The configuration object containing the Calendar's arguments
	* @return {YAHOO.widget.Calendar}	The YAHOO.widget.Calendar instance that is constructed
	*/
	constructChild : function(id,containerId,config) {
		var container = document.getElementById(containerId);
		if (! container) {
			container = document.createElement("div");
			container.id = containerId;
			this.oDomContainer.appendChild(container);
		}
		return new YAHOO.widget.Calendar(id,containerId,config);
	},
	
	/**
	* Sets the calendar group's month explicitly. This month will be set into the first
	* page of the multi-page calendar, and all other months will be iterated appropriately.
	* @method setMonth
	* @param {Number}	month		The numeric month, from 0 (January) to 11 (December)
	*/
	setMonth : function(month) {
		month = parseInt(month, 10);
		var currYear;

		var cfgPageDate = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGEDATE.key;

		for (var p=0; p<this.pages.length; ++p) {
			var cal = this.pages[p];
			var pageDate = cal.cfg.getProperty(cfgPageDate);
			if (p === 0) {
				currYear = pageDate.getFullYear();
			} else {
				pageDate.setFullYear(currYear);
			}
			this._setMonthOnDate(pageDate, month+p); 
			cal.cfg.setProperty(cfgPageDate, pageDate);
		}
	},

	/**
	* Sets the calendar group's year explicitly. This year will be set into the first
	* page of the multi-page calendar, and all other months will be iterated appropriately.
	* @method setYear
	* @param {Number}	year		The numeric 4-digit year
	*/
	setYear : function(year) {
	
		var cfgPageDate = YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGEDATE.key;
	
		year = parseInt(year, 10);
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			var pageDate = cal.cfg.getProperty(cfgPageDate);
	
			if ((pageDate.getMonth()+1) == 1 && p>0) {
				year+=1;
			}
			cal.setYear(year);
		}
	},

	/**
	* Calls the render function of all child calendars within the group.
	* @method render
	*/
	render : function() {
		this.renderHeader();
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.render();
		}
		this.renderFooter();
	},

	/**
	* Selects a date or a collection of dates on the current calendar. This method, by default,
	* does not call the render method explicitly. Once selection has completed, render must be 
	* called for the changes to be reflected visually.
	* @method select
	* @param	{String/Date/Date[]}	date	The date string of dates to select in the current calendar. Valid formats are
	*								individual date(s) (12/24/2005,12/26/2005) or date range(s) (12/24/2005-1/1/2006).
	*								Multiple comma-delimited dates can also be passed to this method (12/24/2005,12/11/2005-12/13/2005).
	*								This method can also take a JavaScript Date object or an array of Date objects.
	* @return	{Date[]}			Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	select : function(date) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.select(date);
		}
		return this.getSelectedDates();
	},

	/**
	* Selects dates in the CalendarGroup based on the cell index provided. This method is used to select cells without having to do a full render. The selected style is applied to the cells directly.
	* The value of the MULTI_SELECT Configuration attribute will determine the set of dates which get selected. 
	* <ul>
	*    <li>If MULTI_SELECT is false, selectCell will select the cell at the specified index for only the last displayed Calendar page.</li>
	*    <li>If MULTI_SELECT is true, selectCell will select the cell at the specified index, on each displayed Calendar page.</li>
	* </ul>
	* @method selectCell
	* @param	{Number}	cellIndex	The index of the cell to be selected. 
	* @return	{Date[]}	Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	selectCell : function(cellIndex) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.selectCell(cellIndex);
		}
		return this.getSelectedDates();
	},
	
	/**
	* Deselects a date or a collection of dates on the current calendar. This method, by default,
	* does not call the render method explicitly. Once deselection has completed, render must be 
	* called for the changes to be reflected visually.
	* @method deselect
	* @param	{String/Date/Date[]}	date	The date string of dates to deselect in the current calendar. Valid formats are
	*								individual date(s) (12/24/2005,12/26/2005) or date range(s) (12/24/2005-1/1/2006).
	*								Multiple comma-delimited dates can also be passed to this method (12/24/2005,12/11/2005-12/13/2005).
	*								This method can also take a JavaScript Date object or an array of Date objects.	
	* @return	{Date[]}			Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	deselect : function(date) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.deselect(date);
		}
		return this.getSelectedDates();
	},
	
	/**
	* Deselects all dates on the current calendar.
	* @method deselectAll
	* @return {Date[]}		Array of JavaScript Date objects representing all individual dates that are currently selected.
	*						Assuming that this function executes properly, the return value should be an empty array.
	*						However, the empty array is returned for the sake of being able to check the selection status
	*						of the calendar.
	*/
	deselectAll : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.deselectAll();
		}
		return this.getSelectedDates();
	},
	
	/**
	* Deselects dates in the CalendarGroup based on the cell index provided. This method is used to select cells without having to do a full render. The selected style is applied to the cells directly.
	* deselectCell will deselect the cell at the specified index on each displayed Calendar page.
	*
	* @method deselectCell
	* @param	{Number}	cellIndex	The index of the cell to deselect. 
	* @return	{Date[]}	Array of JavaScript Date objects representing all individual dates that are currently selected.
	*/
	deselectCell : function(cellIndex) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.deselectCell(cellIndex);
		}
		return this.getSelectedDates();
	},
	
	/**
	* Resets the calendar widget to the originally selected month and year, and 
	* sets the calendar to the initial selection(s).
	* @method reset
	*/
	reset : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.reset();
		}
	},
	
	/**
	* Clears the selected dates in the current calendar widget and sets the calendar
	* to the current month and year.
	* @method clear
	*/
	clear : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.clear();
		}
	},
	
	/**
	* Navigates to the next month page in the calendar widget.
	* @method nextMonth
	*/
	nextMonth : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.nextMonth();
		}
	},
	
	/**
	* Navigates to the previous month page in the calendar widget.
	* @method previousMonth
	*/
	previousMonth : function() {
		for (var p=this.pages.length-1;p>=0;--p) {
			var cal = this.pages[p];
			cal.previousMonth();
		}
	},
	
	/**
	* Navigates to the next year in the currently selected month in the calendar widget.
	* @method nextYear
	*/
	nextYear : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.nextYear();
		}
	},

	/**
	* Navigates to the previous year in the currently selected month in the calendar widget.
	* @method previousYear
	*/
	previousYear : function() {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.previousYear();
		}
	},

	/**
	* Gets the list of currently selected dates from the calendar.
	* @return			An array of currently selected JavaScript Date objects.
	* @type Date[]
	*/
	getSelectedDates : function() { 
		var returnDates = [];
		var selected = this.cfg.getProperty(YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.SELECTED.key);
		for (var d=0;d<selected.length;++d) {
			var dateArray = selected[d];

			var date = YAHOO.widget.DateMath.getDate(dateArray[0],dateArray[1]-1,dateArray[2]);
			returnDates.push(date);
		}

		returnDates.sort( function(a,b) { return a-b; } );
		return returnDates;
	},

	/**
	* Adds a renderer to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the conditions specified in the date string for this renderer.
	* @method addRenderer
	* @param	{String}	sDates		A date string to associate with the specified renderer. Valid formats
	*									include date (12/24/2005), month/day (12/24), and range (12/1/2004-1/1/2005)
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addRenderer : function(sDates, fnRender) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.addRenderer(sDates, fnRender);
		}
	},

	/**
	* Adds a month to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the month passed to this method.
	* @method addMonthRenderer
	* @param	{Number}	month		The month (1-12) to associate with this renderer
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addMonthRenderer : function(month, fnRender) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.addMonthRenderer(month, fnRender);
		}
	},

	/**
	* Adds a weekday to the render stack. The function reference passed to this method will be executed
	* when a date cell matches the weekday passed to this method.
	* @method addWeekdayRenderer
	* @param	{Number}	weekday		The weekday (1-7) to associate with this renderer. 1=Sunday, 2=Monday etc.
	* @param	{Function}	fnRender	The function executed to render cells that match the render rules for this renderer.
	*/
	addWeekdayRenderer : function(weekday, fnRender) {
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			cal.addWeekdayRenderer(weekday, fnRender);
		}
	},

	/**
	 * Removes all custom renderers added to the CalendarGroup through the addRenderer, addMonthRenderer and 
	 * addWeekRenderer methods. CalendarGroup's render method needs to be called to after removing renderers 
	 * to see the changes applied.
	 * 
	 * @method removeRenderers
	 */
	removeRenderers : function() {
		this.callChildFunction("removeRenderers");
	},

	/**
	* Renders the header for the CalendarGroup.
	* @method renderHeader
	*/
	renderHeader : function() {
		// EMPTY DEFAULT IMPL
	},

	/**
	* Renders a footer for the 2-up calendar container. By default, this method is
	* unimplemented.
	* @method renderFooter
	*/
	renderFooter : function() {
		// EMPTY DEFAULT IMPL
	},

	/**
	* Adds the designated number of months to the current calendar month, and sets the current
	* calendar page date to the new month.
	* @method addMonths
	* @param {Number}	count	The number of months to add to the current calendar
	*/
	addMonths : function(count) {
		this.callChildFunction("addMonths", count);
	},
	
	/**
	* Subtracts the designated number of months from the current calendar month, and sets the current
	* calendar page date to the new month.
	* @method subtractMonths
	* @param {Number}	count	The number of months to subtract from the current calendar
	*/
	subtractMonths : function(count) {
		this.callChildFunction("subtractMonths", count);
	},

	/**
	* Adds the designated number of years to the current calendar, and sets the current
	* calendar page date to the new month.
	* @method addYears
	* @param {Number}	count	The number of years to add to the current calendar
	*/
	addYears : function(count) {
		this.callChildFunction("addYears", count);
	},

	/**
	* Subtcats the designated number of years from the current calendar, and sets the current
	* calendar page date to the new month.
	* @method subtractYears
	* @param {Number}	count	The number of years to subtract from the current calendar
	*/
	subtractYears : function(count) {
		this.callChildFunction("subtractYears", count);
	},

	/**
	 * Returns the Calendar page instance which has a pagedate (month/year) matching the given date. 
	 * Returns null if no match is found.
	 * 
	 * @method getCalendarPage
	 * @param {Date} date The JavaScript Date object for which a Calendar page is to be found.
	 * @return {Calendar} The Calendar page instance representing the month to which the date 
	 * belongs.
	 */
	getCalendarPage : function(date) {
		var cal = null;
		if (date) {
			var y = date.getFullYear(),
				m = date.getMonth();

			var pages = this.pages;
			for (var i = 0; i < pages.length; ++i) {
				var pageDate = pages[i].cfg.getProperty("pagedate");
				if (pageDate.getFullYear() === y && pageDate.getMonth() === m) {
					cal = pages[i];
					break;
				}
			}
		}
		return cal;
	},

	/**
	* Sets the month on a Date object, taking into account year rollover if the month is less than 0 or greater than 11.
	* The Date object passed in is modified. It should be cloned before passing it into this method if the original value needs to be maintained
	* @method	_setMonthOnDate
	* @private
	* @param	{Date}	date	The Date object on which to set the month index
	* @param	{Number}	iMonth	The month index to set
	*/
	_setMonthOnDate : function(date, iMonth) {
		// Bug in Safari 1.3, 2.0 (WebKit build < 420), Date.setMonth does not work consistently if iMonth is not 0-11
		if (YAHOO.env.ua.webkit && YAHOO.env.ua.webkit < 420 && (iMonth < 0 || iMonth > 11)) {
			var DM = YAHOO.widget.DateMath;
			var newDate = DM.add(date, DM.MONTH, iMonth-date.getMonth());
			date.setTime(newDate.getTime());
		} else {
			date.setMonth(iMonth);
		}
	},
	
	/**
	 * Fixes the width of the CalendarGroup container element, to account for miswrapped floats
	 * @method _fixWidth
	 * @private
	 */
	_fixWidth : function() {
		var w = 0;
		for (var p=0;p<this.pages.length;++p) {
			var cal = this.pages[p];
			w += cal.oDomContainer.offsetWidth;
		}
		if (w > 0) {
			this.oDomContainer.style.width = w + "px";
		}
	},
	
	/**
	* Returns a string representation of the object.
	* @method toString
	* @return {String}	A string representation of the CalendarGroup object.
	*/
	toString : function() {
		return "CalendarGroup " + this.id;
	}
};

/**
* CSS class representing the container for the calendar
* @property YAHOO.widget.CalendarGroup.CSS_CONTAINER
* @static
* @final
* @type String
*/
YAHOO.widget.CalendarGroup.CSS_CONTAINER = "yui-calcontainer";

/**
* CSS class representing the container for the calendar
* @property YAHOO.widget.CalendarGroup.CSS_MULTI_UP
* @static
* @final
* @type String
*/
YAHOO.widget.CalendarGroup.CSS_MULTI_UP = "multi";

/**
* CSS class representing the title for the 2-up calendar
* @property YAHOO.widget.CalendarGroup.CSS_2UPTITLE
* @static
* @final
* @type String
*/
YAHOO.widget.CalendarGroup.CSS_2UPTITLE = "title";

/**
* CSS class representing the close icon for the 2-up calendar
* @property YAHOO.widget.CalendarGroup.CSS_2UPCLOSE
* @static
* @final
* @deprecated	Along with Calendar.IMG_ROOT and NAV_ARROW_LEFT, NAV_ARROW_RIGHT configuration properties.
*					Calendar's <a href="YAHOO.widget.Calendar.html#Style.CSS_CLOSE">Style.CSS_CLOSE</a> property now represents the CSS class used to render the close icon
* @type String
*/
YAHOO.widget.CalendarGroup.CSS_2UPCLOSE = "close-icon";

YAHOO.lang.augmentProto(YAHOO.widget.CalendarGroup, YAHOO.widget.Calendar, "buildDayLabel",
																 "buildMonthLabel",
																 "renderOutOfBoundsDate",
																 "renderRowHeader",
																 "renderRowFooter",
																 "renderCellDefault",
																 "styleCellDefault",
																 "renderCellStyleHighlight1",
																 "renderCellStyleHighlight2",
																 "renderCellStyleHighlight3",
																 "renderCellStyleHighlight4",
																 "renderCellStyleToday",
																 "renderCellStyleSelected",
																 "renderCellNotThisMonth",
																 "renderBodyCellRestricted",
																 "initStyles",
																 "configTitle",
																 "configClose",
																 "configIframe",
																 "configNavigator",
																 "createTitleBar",
																 "createCloseButton",
																 "removeTitleBar",
																 "removeCloseButton",
																 "hide",
																 "show",
																 "toDate",
																 "_parseArgs",
																 "browser");

/**
* The set of default Config property keys and values for the CalendarGroup
* @property YAHOO.widget.CalendarGroup._DEFAULT_CONFIG
* @final
* @static
* @private
* @type Object
*/
YAHOO.widget.CalendarGroup._DEFAULT_CONFIG = YAHOO.widget.Calendar._DEFAULT_CONFIG;
YAHOO.widget.CalendarGroup._DEFAULT_CONFIG.PAGES = {key:"pages", value:2};

YAHOO.widget.CalGrp = YAHOO.widget.CalendarGroup;

/**
* @class YAHOO.widget.Calendar2up
* @extends YAHOO.widget.CalendarGroup
* @deprecated The old Calendar2up class is no longer necessary, since CalendarGroup renders in a 2up view by default.
*/
YAHOO.widget.Calendar2up = function(id, containerId, config) {
	this.init(id, containerId, config);
};

YAHOO.extend(YAHOO.widget.Calendar2up, YAHOO.widget.CalendarGroup);

/**
* @deprecated The old Calendar2up class is no longer necessary, since CalendarGroup renders in a 2up view by default.
*/
YAHOO.widget.Cal2up = YAHOO.widget.Calendar2up;

/**
 * The CalendarNavigator is used along with a Calendar/CalendarGroup to 
 * provide a Month/Year popup navigation control, allowing the user to navigate 
 * to a specific month/year in the Calendar/CalendarGroup without having to 
 * scroll through months sequentially
 *
 * @namespace YAHOO.widget
 * @class CalendarNavigator
 * @constructor
 * @param {Calendar|CalendarGroup} cal The instance of the Calendar or CalendarGroup to which this CalendarNavigator should be attached.
 */
YAHOO.widget.CalendarNavigator = function(cal) {
	this.init(cal);
};

(function() {
	// Setup static properties (inside anon fn, so that we can use shortcuts)
	var CN = YAHOO.widget.CalendarNavigator;

	/**
	 * YAHOO.widget.CalendarNavigator.CLASSES contains constants
	 * for the class values applied to the CalendarNaviatgator's 
	 * DOM elements
	 * @property YAHOO.widget.CalendarNavigator.CLASSES
	 * @type Object
	 * @static
	 */
	CN.CLASSES = {
		/**
		 * Class applied to the Calendar Navigator's bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.NAV
		 * @type String
		 * @static
		 */
		NAV :"yui-cal-nav",
		/**
		 * Class applied to the Calendar/CalendarGroup's bounding box to indicate
		 * the Navigator is currently visible
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.NAV_VISIBLE
		 * @type String
		 * @static
		 */
		NAV_VISIBLE: "yui-cal-nav-visible",
		/**
		 * Class applied to the Navigator mask's bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.MASK
		 * @type String
		 * @static
		 */
		MASK : "yui-cal-nav-mask",
		/**
		 * Class applied to the year label/control bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.YEAR
		 * @type String
		 * @static
		 */
		YEAR : "yui-cal-nav-y",
		/**
		 * Class applied to the month label/control bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.MONTH
		 * @type String
		 * @static
		 */
		MONTH : "yui-cal-nav-m",
		/**
		 * Class applied to the submit/cancel button's bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.BUTTONS
		 * @type String
		 * @static
		 */
		BUTTONS : "yui-cal-nav-b",
		/**
		 * Class applied to buttons wrapping element
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.BUTTON
		 * @type String
		 * @static
		 */
		BUTTON : "yui-cal-nav-btn",
		/**
		 * Class applied to the validation error area's bounding box
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.ERROR
		 * @type String
		 * @static
		 */
		ERROR : "yui-cal-nav-e",
		/**
		 * Class applied to the year input control
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.YEAR_CTRL
		 * @type String
		 * @static
		 */
		YEAR_CTRL : "yui-cal-nav-yc",
		/**
		 * Class applied to the month input control
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.MONTH_CTRL
		 * @type String
		 * @static
		 */
		MONTH_CTRL : "yui-cal-nav-mc",
		/**
		 * Class applied to controls with invalid data (e.g. a year input field with invalid an year)
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.INVALID
		 * @type String
		 * @static
		 */
		INVALID : "yui-invalid",
		/**
		 * Class applied to default controls
		 * @property YAHOO.widget.CalendarNavigator.CLASSES.DEFAULT
		 * @type String
		 * @static
		 */
		DEFAULT : "yui-default"
	};

	/**
	 * Object literal containing the default configuration values for the CalendarNavigator
	 * The configuration object is expected to follow the format below, with the properties being
	 * case sensitive.
	 * <dl>
	 * <dt>strings</dt>
	 * <dd><em>Object</em> :  An object with the properties shown below, defining the string labels to use in the Navigator's UI
	 *     <dl>
	 *         <dt>month</dt><dd><em>String</em> : The string to use for the month label. Defaults to "Month".</dd>
	 *         <dt>year</dt><dd><em>String</em> : The string to use for the year label. Defaults to "Year".</dd>
	 *         <dt>submit</dt><dd><em>String</em> : The string to use for the submit button label. Defaults to "Okay".</dd>
	 *         <dt>cancel</dt><dd><em>String</em> : The string to use for the cancel button label. Defaults to "Cancel".</dd>
	 *         <dt>invalidYear</dt><dd><em>String</em> : The string to use for invalid year values. Defaults to "Year needs to be a number".</dd>
	 *     </dl>
	 * </dd>
	 * <dt>monthFormat</dt><dd><em>String</em> : The month format to use. Either YAHOO.widget.Calendar.LONG, or YAHOO.widget.Calendar.SHORT. Defaults to YAHOO.widget.Calendar.LONG</dd>
	 * <dt>initialFocus</dt><dd><em>String</em> : Either "year" or "month" specifying which input control should get initial focus. Defaults to "year"</dd>
	 * </dl>
	 * @property _DEFAULT_CFG
	 * @protected
	 * @type Object
	 * @static
	 */
	CN._DEFAULT_CFG = {
		strings : {
			month: "Month",
			year: "Year",
			submit: "Okay",
			cancel: "Cancel",
			invalidYear : "Year needs to be a number"
		},
		monthFormat: YAHOO.widget.Calendar.LONG,
		initialFocus: "year"
	};

	/**
	 * The suffix added to the Calendar/CalendarGroup's ID, to generate
	 * a unique ID for the Navigator and it's bounding box.
	 * @property YAHOO.widget.CalendarNavigator.ID_SUFFIX
	 * @static
	 * @type String
	 * @final
	 */
	CN.ID_SUFFIX = "_nav";
	/**
	 * The suffix added to the Navigator's ID, to generate
	 * a unique ID for the month control.
	 * @property YAHOO.widget.CalendarNavigator.MONTH_SUFFIX
	 * @static
	 * @type String 
	 * @final
	 */
	CN.MONTH_SUFFIX = "_month";
	/**
	 * The suffix added to the Navigator's ID, to generate
	 * a unique ID for the year control.
	 * @property YAHOO.widget.CalendarNavigator.YEAR_SUFFIX
	 * @static
	 * @type String
	 * @final
	 */
	CN.YEAR_SUFFIX = "_year";
	/**
	 * The suffix added to the Navigator's ID, to generate
	 * a unique ID for the error bounding box.
	 * @property YAHOO.widget.CalendarNavigator.ERROR_SUFFIX
	 * @static
	 * @type String
	 * @final
	 */
	CN.ERROR_SUFFIX = "_error";
	/**
	 * The suffix added to the Navigator's ID, to generate
	 * a unique ID for the "Cancel" button.
	 * @property YAHOO.widget.CalendarNavigator.CANCEL_SUFFIX
	 * @static
	 * @type String
	 * @final
	 */
	CN.CANCEL_SUFFIX = "_cancel";
	/**
	 * The suffix added to the Navigator's ID, to generate
	 * a unique ID for the "Submit" button.
	 * @property YAHOO.widget.CalendarNavigator.SUBMIT_SUFFIX
	 * @static
	 * @type String
	 * @final
	 */
	CN.SUBMIT_SUFFIX = "_submit";

	/**
	 * The number of digits to which the year input control is to be limited.
	 * @property YAHOO.widget.CalendarNavigator.YR_MAX_DIGITS
	 * @static
	 * @type Number
	 */
	CN.YR_MAX_DIGITS = 4;

	/**
	 * The amount by which to increment the current year value,
	 * when the arrow up/down key is pressed on the year control
	 * @property YAHOO.widget.CalendarNavigator.YR_MINOR_INC
	 * @static
	 * @type Number
	 */
	CN.YR_MINOR_INC = 1;

	/**
	 * The amount by which to increment the current year value,
	 * when the page up/down key is pressed on the year control
	 * @property YAHOO.widget.CalendarNavigator.YR_MAJOR_INC
	 * @static
	 * @type Number
	 */
	CN.YR_MAJOR_INC = 10;

	/**
	 * Artificial delay (in ms) between the time the Navigator is hidden
	 * and the Calendar/CalendarGroup state is updated. Allows the user
	 * the see the Calendar/CalendarGroup page changing. If set to 0
	 * the Calendar/CalendarGroup page will be updated instantly
	 * @property YAHOO.widget.CalendarNavigator.UPDATE_DELAY
	 * @static
	 * @type Number
	 */
	CN.UPDATE_DELAY = 50;

	/**
	 * Regular expression used to validate the year input
	 * @property YAHOO.widget.CalendarNavigator.YR_PATTERN
	 * @static
	 * @type RegExp
	 */
	CN.YR_PATTERN = /^\d+$/;
	/**
	 * Regular expression used to trim strings
	 * @property YAHOO.widget.CalendarNavigator.TRIM
	 * @static
	 * @type RegExp
	 */
	CN.TRIM = /^\s*(.*?)\s*$/;
})();

YAHOO.widget.CalendarNavigator.prototype = {

	/**
	 * The unique ID for this CalendarNavigator instance
	 * @property id
	 * @type String
	 */
	id : null,

	/**
	 * The Calendar/CalendarGroup instance to which the navigator belongs
	 * @property cal
	 * @type {Calendar|CalendarGroup}
	 */
	cal : null,

	/**
	 * Reference to the HTMLElement used to render the navigator's bounding box
	 * @property navEl
	 * @type HTMLElement
	 */
	navEl : null,

	/**
	 * Reference to the HTMLElement used to render the navigator's mask
	 * @property maskEl
	 * @type HTMLElement
	 */
	maskEl : null,

	/**
	 * Reference to the HTMLElement used to input the year
	 * @property yearEl
	 * @type HTMLElement
	 */
	yearEl : null,

	/**
	 * Reference to the HTMLElement used to input the month
	 * @property monthEl
	 * @type HTMLElement
	 */
	monthEl : null,

	/**
	 * Reference to the HTMLElement used to display validation errors
	 * @property errorEl
	 * @type HTMLElement
	 */
	errorEl : null,

	/**
	 * Reference to the HTMLElement used to update the Calendar/Calendar group
	 * with the month/year values
	 * @property submitEl
	 * @type HTMLElement
	 */
	submitEl : null,
	
	/**
	 * Reference to the HTMLElement used to hide the navigator without updating the 
	 * Calendar/Calendar group
	 * @property cancelEl
	 * @type HTMLElement
	 */
	cancelEl : null,

	/** 
	 * Reference to the first focusable control in the navigator (by default monthEl)
	 * @property firstCtrl
	 * @type HTMLElement
	 */
	firstCtrl : null,
	
	/** 
	 * Reference to the last focusable control in the navigator (by default cancelEl)
	 * @property lastCtrl
	 * @type HTMLElement
	 */
	lastCtrl : null,

	/**
	 * The document containing the Calendar/Calendar group instance
	 * @protected
	 * @property _doc
	 * @type HTMLDocument
	 */
	_doc : null,

	/**
	 * Internal state property for the current year displayed in the navigator
	 * @protected
	 * @property _year
	 * @type Number
	 */
	_year: null,
	
	/**
	 * Internal state property for the current month index displayed in the navigator
	 * @protected
	 * @property _month
	 * @type Number
	 */
	_month: 0,

	/**
	 * Private internal state property which indicates whether or not the 
	 * Navigator has been rendered.
	 * @private
	 * @property __rendered
	 * @type Boolean
	 */
	__rendered: false,

	/**
	 * Init lifecycle method called as part of construction
	 * 
	 * @method init
	 * @param {Calendar} cal The instance of the Calendar or CalendarGroup to which this CalendarNavigator should be attached
	 */
	init : function(cal) {
		var calBox = cal.oDomContainer;

		this.cal = cal;
		this.id = calBox.id + YAHOO.widget.CalendarNavigator.ID_SUFFIX;
		this._doc = calBox.ownerDocument;

		/**
		 * Private flag, to identify IE6/IE7 Quirks
		 * @private
		 * @property __isIEQuirks
		 */
		var ie = YAHOO.env.ua.ie;
		this.__isIEQuirks = (ie && ((ie <= 6) || (ie === 7 && this._doc.compatMode == "BackCompat")));
	},

	/**
	 * Displays the navigator and mask, updating the input controls to reflect the 
	 * currently set month and year. The show method will invoke the render method
	 * if the navigator has not been renderered already, allowing for lazy rendering
	 * of the control.
	 * 
	 * The show method will fire the Calendar/CalendarGroup's beforeShowNav and showNav events
	 * 
	 * @method show
	 */
	show : function() {
		var CLASSES = YAHOO.widget.CalendarNavigator.CLASSES;

		if (this.cal.beforeShowNavEvent.fire()) {
			if (!this.__rendered) {
				this.render();
			}
			this.clearErrors();

			this._updateMonthUI();
			this._updateYearUI();
			this._show(this.navEl, true);

			this.setInitialFocus();
			this.showMask();

			YAHOO.util.Dom.addClass(this.cal.oDomContainer, CLASSES.NAV_VISIBLE);
			this.cal.showNavEvent.fire();
		}
	},

	/**
	 * Hides the navigator and mask
	 * 
	 * The show method will fire the Calendar/CalendarGroup's beforeHideNav event and hideNav events
	 * @method hide
	 */
	hide : function() {
		var CLASSES = YAHOO.widget.CalendarNavigator.CLASSES;

		if (this.cal.beforeHideNavEvent.fire()) {
			this._show(this.navEl, false);
			this.hideMask();
			YAHOO.util.Dom.removeClass(this.cal.oDomContainer, CLASSES.NAV_VISIBLE);
			this.cal.hideNavEvent.fire();
		}
	},
	

	/**
	 * Displays the navigator's mask element
	 * 
	 * @method showMask
	 */
	showMask : function() {
		this._show(this.maskEl, true);
		if (this.__isIEQuirks) {
			this._syncMask();
		}
	},

	/**
	 * Hides the navigator's mask element
	 * 
	 * @method hideMask
	 */
	hideMask : function() {
		this._show(this.maskEl, false);
	},

	/**
	 * Returns the current month set on the navigator
	 * 
	 * Note: This may not be the month set in the UI, if 
	 * the UI contains an invalid value.
	 * 
	 * @method getMonth
	 * @return {Number} The Navigator's current month index
	 */
	getMonth: function() {
		return this._month;
	},

	/**
	 * Returns the current year set on the navigator
	 * 
	 * Note: This may not be the year set in the UI, if 
	 * the UI contains an invalid value.
	 * 
	 * @method getYear
	 * @return {Number} The Navigator's current year value
	 */
	getYear: function() {
		return this._year;
	},

	/**
	 * Sets the current month on the Navigator, and updates the UI
	 * 
	 * @method setMonth
	 * @param {Number} nMonth The month index, from 0 (Jan) through 11 (Dec).
	 */
	setMonth : function(nMonth) {
		if (nMonth >= 0 && nMonth < 12) {
			this._month = nMonth;
		}
		this._updateMonthUI();
	},

	/**
	 * Sets the current year on the Navigator, and updates the UI. If the 
	 * provided year is invalid, it will not be set.
	 * 
	 * @method setYear
	 * @param {Number} nYear The full year value to set the Navigator to.
	 */
	setYear : function(nYear) {
		var yrPattern = YAHOO.widget.CalendarNavigator.YR_PATTERN;
		if (YAHOO.lang.isNumber(nYear) && yrPattern.test(nYear+"")) {
			this._year = nYear;
		}
		this._updateYearUI();
	},

	/**
	 * Renders the HTML for the navigator, adding it to the 
	 * document and attaches event listeners if it has not 
	 * already been rendered.
	 * 
	 * @method render
	 */
	render: function() {
		this.cal.beforeRenderNavEvent.fire();
		if (!this.__rendered) {
			this.createNav();
			this.createMask();
			this.applyListeners();
			this.__rendered = true;
		}
		this.cal.renderNavEvent.fire();
	},

	/**
	 * Creates the navigator's containing HTMLElement, it's contents, and appends 
	 * the containg element to the Calendar/CalendarGroup's container.
	 * 
	 * @method createNav
	 */
	createNav : function() {
		var NAV = YAHOO.widget.CalendarNavigator;
		var doc = this._doc;

		var d = doc.createElement("div");
		d.className = NAV.CLASSES.NAV;

		var htmlBuf = this.renderNavContents([]);

		d.innerHTML = htmlBuf.join('');
		this.cal.oDomContainer.appendChild(d);

		this.navEl = d;

		this.yearEl = doc.getElementById(this.id + NAV.YEAR_SUFFIX);
		this.monthEl = doc.getElementById(this.id + NAV.MONTH_SUFFIX);
		this.errorEl = doc.getElementById(this.id + NAV.ERROR_SUFFIX);
		this.submitEl = doc.getElementById(this.id + NAV.SUBMIT_SUFFIX);
		this.cancelEl = doc.getElementById(this.id + NAV.CANCEL_SUFFIX);

		if (YAHOO.env.ua.gecko && this.yearEl && this.yearEl.type == "text") {
			// Avoid XUL error on focus, select [ https://bugzilla.mozilla.org/show_bug.cgi?id=236791, 
			// supposedly fixed in 1.8.1, but there are reports of it still being around for methods other than blur ]
			this.yearEl.setAttribute("autocomplete", "off");
		}

		this._setFirstLastElements();
	},

	/**
	 * Creates the Mask HTMLElement and appends it to the Calendar/CalendarGroups
	 * container.
	 * 
	 * @method createMask
	 */
	createMask : function() {
		var C = YAHOO.widget.CalendarNavigator.CLASSES;

		var d = this._doc.createElement("div");
		d.className = C.MASK;

		this.cal.oDomContainer.appendChild(d);
		this.maskEl = d;
	},

	/**
	 * Used to set the width/height of the mask in pixels to match the Calendar Container.
	 * Currently only used for IE6 and IE7 quirks mode. The other A-Grade browser are handled using CSS (width/height 100%).
	 * <p>
	 * The method is also registered as an HTMLElement resize listener on the Calendars container element.
	 * </p>
	 * @protected
	 * @method _syncMask
	 */
	_syncMask : function() {
		var c = this.cal.oDomContainer;
		if (c && this.maskEl) {
			var r = YAHOO.util.Dom.getRegion(c);
			YAHOO.util.Dom.setStyle(this.maskEl, "width", r.right - r.left + "px");
			YAHOO.util.Dom.setStyle(this.maskEl, "height", r.bottom - r.top + "px");
		}
	},

	/**
	 * Renders the contents of the navigator
	 * 
	 * @method renderNavContents
	 * 
	 * @param {Array} html The HTML buffer to append the HTML to.
	 * @return {Array} A reference to the buffer passed in.
	 */
	renderNavContents : function(html) {
		var NAV = YAHOO.widget.CalendarNavigator,
			C = NAV.CLASSES,
			h = html; // just to use a shorter name

		h[h.length] = '<div class="' + C.MONTH + '">';
		this.renderMonth(h);
		h[h.length] = '</div>';
		h[h.length] = '<div class="' + C.YEAR + '">';
		this.renderYear(h);
		h[h.length] = '</div>';
		h[h.length] = '<div class="' + C.BUTTONS + '">';
		this.renderButtons(h);
		h[h.length] = '</div>';
		h[h.length] = '<div class="' + C.ERROR + '" id="' + this.id + NAV.ERROR_SUFFIX + '"></div>';

		return h;
	},

	/**
	 * Renders the month label and control for the navigator
	 * 
	 * @method renderNavContents
	 * @param {Array} html The HTML buffer to append the HTML to.
	 * @return {Array} A reference to the buffer passed in.
	 */
	renderMonth : function(html) {
		var NAV = YAHOO.widget.CalendarNavigator,
			C = NAV.CLASSES;

		var id = this.id + NAV.MONTH_SUFFIX,
			mf = this.__getCfg("monthFormat"),
			months = this.cal.cfg.getProperty((mf == YAHOO.widget.Calendar.SHORT) ? "MONTHS_SHORT" : "MONTHS_LONG"),
			h = html;

		if (months && months.length > 0) {
			h[h.length] = '<label for="' + id + '">';
			h[h.length] = this.__getCfg("month", true);
			h[h.length] = '</label>';
			h[h.length] = '<select name="' + id + '" id="' + id + '" class="' + C.MONTH_CTRL + '">';
			for (var i = 0; i < months.length; i++) {
				h[h.length] = '<option value="' + i + '">';
				h[h.length] = months[i];
				h[h.length] = '</option>';
			}
			h[h.length] = '</select>';
		}
		return h;
	},

	/**
	 * Renders the year label and control for the navigator
	 * 
	 * @method renderYear
	 * @param {Array} html The HTML buffer to append the HTML to.
	 * @return {Array} A reference to the buffer passed in.
	 */
	renderYear : function(html) {
		var NAV = YAHOO.widget.CalendarNavigator,
			C = NAV.CLASSES;

		var id = this.id + NAV.YEAR_SUFFIX,
			size = NAV.YR_MAX_DIGITS,
			h = html;

		h[h.length] = '<label for="' + id + '">';
		h[h.length] = this.__getCfg("year", true);
		h[h.length] = '</label>';
		h[h.length] = '<input type="text" name="' + id + '" id="' + id + '" class="' + C.YEAR_CTRL + '" maxlength="' + size + '"/>';
		return h;
	},

	/**
	 * Renders the submit/cancel buttons for the navigator
	 * 
	 * @method renderButton
	 * @return {String} The HTML created for the Button UI
	 */
	renderButtons : function(html) {
		var C = YAHOO.widget.CalendarNavigator.CLASSES;
		var h = html;

		h[h.length] = '<span class="' + C.BUTTON + ' ' + C.DEFAULT + '">';
		h[h.length] = '<button type="button" id="' + this.id + '_submit' + '">';
		h[h.length] = this.__getCfg("submit", true);
		h[h.length] = '</button>';
		h[h.length] = '</span>';
		h[h.length] = '<span class="' + C.BUTTON +'">';
		h[h.length] = '<button type="button" id="' + this.id + '_cancel' + '">';
		h[h.length] = this.__getCfg("cancel", true);
		h[h.length] = '</button>';
		h[h.length] = '</span>';

		return h;
	},

	/**
	 * Attaches DOM event listeners to the rendered elements
	 * <p>
	 * The method will call applyKeyListeners, to setup keyboard specific 
	 * listeners
	 * </p>
	 * @method applyListeners
	 */
	applyListeners : function() {
		var E = YAHOO.util.Event;

		function yearUpdateHandler() {
			if (this.validate()) {
				this.setYear(this._getYearFromUI());
			}
		}

		function monthUpdateHandler() {
			this.setMonth(this._getMonthFromUI());
		}

		E.on(this.submitEl, "click", this.submit, this, true);
		E.on(this.cancelEl, "click", this.cancel, this, true);
		E.on(this.yearEl, "blur", yearUpdateHandler, this, true);
		E.on(this.monthEl, "change", monthUpdateHandler, this, true);

		if (this.__isIEQuirks) {
			YAHOO.util.Event.on(this.cal.oDomContainer, "resize", this._syncMask, this, true);
		}

		this.applyKeyListeners();
	},

	/**
	 * Removes/purges DOM event listeners from the rendered elements
	 * 
	 * @method purgeListeners
	 */
	purgeListeners : function() {
		var E = YAHOO.util.Event;
		E.removeListener(this.submitEl, "click", this.submit);
		E.removeListener(this.cancelEl, "click", this.cancel);
		E.removeListener(this.yearEl, "blur");
		E.removeListener(this.monthEl, "change");
		if (this.__isIEQuirks) {
			E.removeListener(this.cal.oDomContainer, "resize", this._syncMask);
		}

		this.purgeKeyListeners();
	},

	/**
	 * Attaches DOM listeners for keyboard support. 
	 * Tab/Shift-Tab looping, Enter Key Submit on Year element,
	 * Up/Down/PgUp/PgDown year increment on Year element
	 * <p>
	 * NOTE: MacOSX Safari 2.x doesn't let you tab to buttons and 
	 * MacOSX Gecko does not let you tab to buttons or select controls,
	 * so for these browsers, Tab/Shift-Tab looping is limited to the 
	 * elements which can be reached using the tab key.
	 * </p>
	 * @method applyKeyListeners
	 */
	applyKeyListeners : function() {
		var E = YAHOO.util.Event;

		// IE doesn't fire keypress for arrow/pg keys (non-char keys)
		var ua = YAHOO.env.ua;
		var arrowEvt = (ua.ie) ? "keydown" : "keypress";

		// - IE doesn't fire keypress for non-char keys
		// - Opera doesn't allow us to cancel keydown or keypress for tab, but 
		//   changes focus successfully on keydown (keypress is too late to change focus - opera's already moved on).
		var tabEvt = (ua.ie || ua.opera) ? "keydown" : "keypress";

		// Everyone likes keypress for Enter (char keys) - whoo hoo!
		E.on(this.yearEl, "keypress", this._handleEnterKey, this, true);

		E.on(this.yearEl, arrowEvt, this._handleDirectionKeys, this, true);
		E.on(this.lastCtrl, tabEvt, this._handleTabKey, this, true);
		E.on(this.firstCtrl, tabEvt, this._handleShiftTabKey, this, true);
	},

	/**
	 * Removes/purges DOM listeners for keyboard support
	 *
	 * @method purgeKeyListeners
	 */
	purgeKeyListeners : function() {
		var E = YAHOO.util.Event;

		var arrowEvt = (YAHOO.env.ua.ie) ? "keydown" : "keypress";
		var tabEvt = (YAHOO.env.ua.ie || YAHOO.env.ua.opera) ? "keydown" : "keypress";

		E.removeListener(this.yearEl, "keypress", this._handleEnterKey);
		E.removeListener(this.yearEl, arrowEvt, this._handleDirectionKeys);
		E.removeListener(this.lastCtrl, tabEvt, this._handleTabKey);
		E.removeListener(this.firstCtrl, tabEvt, this._handleShiftTabKey);
	},

	/**
	 * Updates the Calendar/CalendarGroup's pagedate with the currently set month and year if valid.
	 * <p>
	 * If the currently set month/year is invalid, a validation error will be displayed and the 
	 * Calendar/CalendarGroup's pagedate will not be updated.
	 * </p>
	 * @method submit
	 */
	submit : function() {
		if (this.validate()) {
			this.hide();

			this.setMonth(this._getMonthFromUI());
			this.setYear(this._getYearFromUI());

			var cal = this.cal;
			var nav = this;
			
			function update() {
				cal.setYear(nav.getYear());
				cal.setMonth(nav.getMonth());
				cal.render();
			}
			// Artificial delay, just to help the user see something changed
			var delay = YAHOO.widget.CalendarNavigator.UPDATE_DELAY;
			if (delay > 0) {
				window.setTimeout(update, delay);
			} else {
				update();
			}
		}
	},

	/**
	 * Hides the navigator and mask, without updating the Calendar/CalendarGroup's state
	 * 
	 * @method cancel
	 */
	cancel : function() {
		this.hide();
	},

	/**
	 * Validates the current state of the UI controls
	 * 
	 * @method validate
	 * @return {Boolean} true, if the current UI state contains valid values, false if not
	 */
	validate : function() {
		if (this._getYearFromUI() !== null) {
			this.clearErrors();
			return true;
		} else {
			this.setYearError();
			this.setError(this.__getCfg("invalidYear", true));
			return false;
		}
	},

	/**
	 * Displays an error message in the Navigator's error panel
	 * @method setError
	 * @param {String} msg The error message to display
	 */
	setError : function(msg) {
		if (this.errorEl) {
			this.errorEl.innerHTML = msg;
			this._show(this.errorEl, true);
		}
	},

	/**
	 * Clears the navigator's error message and hides the error panel
	 * @method clearError 
	 */
	clearError : function() {
		if (this.errorEl) {
			this.errorEl.innerHTML = "";
			this._show(this.errorEl, false);
		}
	},

	/**
	 * Displays the validation error UI for the year control
	 * @method setYearError
	 */
	setYearError : function() {
		YAHOO.util.Dom.addClass(this.yearEl, YAHOO.widget.CalendarNavigator.CLASSES.INVALID);
	},

	/**
	 * Removes the validation error UI for the year control
	 * @method clearYearError
	 */
	clearYearError : function() {
		YAHOO.util.Dom.removeClass(this.yearEl, YAHOO.widget.CalendarNavigator.CLASSES.INVALID);
	},

	/**
	 * Clears all validation and error messages in the UI
	 * @method clearErrors
	 */
	clearErrors : function() {
		this.clearError();
		this.clearYearError();
	},

	/**
	 * Sets the initial focus, based on the configured value
	 * @method setInitialFocus
	 */
	setInitialFocus : function() {
		var el = this.submitEl;
		var f = this.__getCfg("initialFocus");

		if (f && f.toLowerCase) {
			f = f.toLowerCase();
			if (f == "year") {
				el = this.yearEl;
				try {
					this.yearEl.select();
				} catch (e) {
					// Ignore;
				}
			} else if (f == "month") {
				el = this.monthEl;
			}
		}

		if (el && YAHOO.lang.isFunction(el.focus)) {
			try {
				el.focus();
			} catch (e) {
				// TODO: Fall back if focus fails?
			}
		}
	},

	/**
	 * Removes all renderered HTML elements for the Navigator from
	 * the DOM, purges event listeners and clears (nulls) any property
	 * references to HTML references
	 * @method erase
	 */
	erase : function() {
		if (this.__rendered) {
			this.purgeListeners();

			// Clear out innerHTML references
			this.yearEl = null;
			this.monthEl = null;
			this.errorEl = null;
			this.submitEl = null;
			this.cancelEl = null;
			this.firstCtrl = null;
			this.lastCtrl = null;
			if (this.navEl) {
				this.navEl.innerHTML = "";
			}

			var p = this.navEl.parentNode;
			if (p) {
				p.removeChild(this.navEl);
			}
			this.navEl = null;

			var pm = this.maskEl.parentNode;
			if (pm) {
				pm.removeChild(this.maskEl);
			}
			this.maskEl = null;
			this.__rendered = false;
		}
	},

	/**
	 * Destroys the Navigator object and any HTML references
	 * @method destroy
	 */
	destroy : function() {
		this.erase();
		this._doc = null;
		this.cal = null;
		this.id = null;
	},

	/**
	 * Protected implementation to handle how UI elements are 
	 * hidden/shown.
	 *
	 * @method _show
	 * @protected
	 */
	_show : function(el, bShow) {
		if (el) {
			YAHOO.util.Dom.setStyle(el, "display", (bShow) ? "block" : "none");
		}
	},

	/**
	 * Returns the month value (index), from the month UI element
	 * @protected
	 * @method _getMonthFromUI
	 * @return {Number} The month index, or 0 if a UI element for the month
	 * is not found
	 */
	_getMonthFromUI : function() {
		if (this.monthEl) {
			return this.monthEl.selectedIndex;
		} else {
			return 0; // Default to Jan
		}
	},

	/**
	 * Returns the year value, from the Navitator's year UI element
	 * @protected
	 * @method _getYearFromUI
	 * @return {Number} The year value set in the UI, if valid. null is returned if 
	 * the UI does not contain a valid year value.
	 */
	_getYearFromUI : function() {
		var NAV = YAHOO.widget.CalendarNavigator;

		var yr = null;
		if (this.yearEl) {
			var value = this.yearEl.value;
			value = value.replace(NAV.TRIM, "$1");

			if (NAV.YR_PATTERN.test(value)) {
				yr = parseInt(value, 10);
			}
		}
		return yr;
	},

	/**
	 * Updates the Navigator's year UI, based on the year value set on the Navigator object
	 * @protected
	 * @method _updateYearUI
	 */
	_updateYearUI : function() {
		if (this.yearEl && this._year !== null) {
			this.yearEl.value = this._year;
		}
	},

	/**
	 * Updates the Navigator's month UI, based on the month value set on the Navigator object
	 * @protected
	 * @method _updateMonthUI
	 */
	_updateMonthUI : function() {
		if (this.monthEl) {
			this.monthEl.selectedIndex = this._month;
		}
	},

	/**
	 * Sets up references to the first and last focusable element in the Navigator's UI
	 * in terms of tab order (Naviagator's firstEl and lastEl properties). The references
	 * are used to control modality by looping around from the first to the last control
	 * and visa versa for tab/shift-tab navigation.
	 * <p>
	 * See <a href="#applyKeyListeners">applyKeyListeners</a>
	 * </p>
	 * @protected
	 * @method _setFirstLastElements
	 */
	_setFirstLastElements : function() {
		this.firstCtrl = this.monthEl;
		this.lastCtrl = this.cancelEl;

		// Special handling for MacOSX.
		// - Safari 2.x can't focus on buttons
		// - Gecko can't focus on select boxes or buttons
		if (this.__isMac) {
			if (YAHOO.env.ua.webkit && YAHOO.env.ua.webkit < 420){
				this.firstCtrl = this.monthEl;
				this.lastCtrl = this.yearEl;
			}
			if (YAHOO.env.ua.gecko) {
				this.firstCtrl = this.yearEl;
				this.lastCtrl = this.yearEl;
			}
		}
	},

	/**
	 * Default Keyboard event handler to capture Enter 
	 * on the Navigator's year control (yearEl)
	 * 
	 * @method _handleEnterKey
	 * @protected
	 * @param {Event} e The DOM event being handled
	 */
	_handleEnterKey : function(e) {
		var KEYS = YAHOO.util.KeyListener.KEY;

		if (YAHOO.util.Event.getCharCode(e) == KEYS.ENTER) {
			this.submit();
		}
	},

	/**
	 * Default Keyboard event handler to capture up/down/pgup/pgdown
	 * on the Navigator's year control (yearEl).
	 * 
	 * @method _handleDirectionKeys
	 * @protected
	 * @param {Event} e The DOM event being handled
	 */
	_handleDirectionKeys : function(e) {
		var E = YAHOO.util.Event;
		var KEYS = YAHOO.util.KeyListener.KEY;
		var NAV = YAHOO.widget.CalendarNavigator;

		var value = (this.yearEl.value) ? parseInt(this.yearEl.value, 10) : null;
		if (isFinite(value)) {
			var dir = false;
			switch(E.getCharCode(e)) {
				case KEYS.UP:
					this.yearEl.value = value + NAV.YR_MINOR_INC;
					dir = true;
					break;
				case KEYS.DOWN:
					this.yearEl.value = Math.max(value - NAV.YR_MINOR_INC, 0);
					dir = true;
					break;
				case KEYS.PAGE_UP:
					this.yearEl.value = value + NAV.YR_MAJOR_INC;
					dir = true;
					break;
				case KEYS.PAGE_DOWN:
					this.yearEl.value = Math.max(value - NAV.YR_MAJOR_INC, 0);
					dir = true;
					break;
				default:
					break;
			}
			if (dir) {
				E.preventDefault(e);
				try {
					this.yearEl.select();
				} catch(e) {
					// Ignore
				}
			}
		}
	},

	/**
	 * Default Keyboard event handler to capture Tab 
	 * on the last control (lastCtrl) in the Navigator.
	 * 
	 * @method _handleTabKey
	 * @protected
	 * @param {Event} e The DOM event being handled
	 */
	_handleTabKey : function(e) {
		var E = YAHOO.util.Event;
		var KEYS = YAHOO.util.KeyListener.KEY;

		if (E.getCharCode(e) == KEYS.TAB && !e.shiftKey) {
			try {
				E.preventDefault(e);
				this.firstCtrl.focus();
			} catch (e) {
				// Ignore - mainly for focus edge cases
			}
		}
	},

	/**
	 * Default Keyboard event handler to capture Shift-Tab 
	 * on the first control (firstCtrl) in the Navigator.
	 * 
	 * @method _handleShiftTabKey
	 * @protected
	 * @param {Event} e The DOM event being handled
	 */
	_handleShiftTabKey : function(e) {
		var E = YAHOO.util.Event;
		var KEYS = YAHOO.util.KeyListener.KEY;

		if (e.shiftKey && E.getCharCode(e) == KEYS.TAB) {
			try {
				E.preventDefault(e);
				this.lastCtrl.focus();
			} catch (e) {
				// Ignore - mainly for focus edge cases
			}
		}
	},

	/**
	 * Retrieve Navigator configuration values from 
	 * the parent Calendar/CalendarGroup's config value.
	 * <p>
	 * If it has not been set in the user provided configuration, the method will 
	 * return the default value of the configuration property, as set in _DEFAULT_CFG
	 * </p>
	 * @private
	 * @method __getCfg
	 * @param {String} Case sensitive property name.
	 * @param {Boolean} true, if the property is a string property, false if not.
	 * @return The value of the configuration property
	 */
	__getCfg : function(prop, bIsStr) {
		var DEF_CFG = YAHOO.widget.CalendarNavigator._DEFAULT_CFG;
		var cfg = this.cal.cfg.getProperty("navigator");

		if (bIsStr) {
			return (cfg !== true && cfg.strings && cfg.strings[prop]) ? cfg.strings[prop] : DEF_CFG.strings[prop];
		} else {
			return (cfg !== true && cfg[prop]) ? cfg[prop] : DEF_CFG[prop];
		}
	},

	/**
	 * Private flag, to identify MacOS
	 * @private
	 * @property __isMac
	 */
	__isMac : (navigator.userAgent.toLowerCase().indexOf("macintosh") != -1)

};

YAHOO.register("calendar", YAHOO.widget.Calendar, {version: "2.4.1", build: "742"});
