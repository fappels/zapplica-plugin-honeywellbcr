/*
   Copyright 2017 Francis Appels - http://www.z-application.com/

   Licensed under the Apache License, Version 2.0 (the "License");
   you may not use this file except in compliance with the License.
   You may obtain a copy of the License at

       http://www.apache.org/licenses/LICENSE-2.0

   Unless required by applicable law or agreed to in writing, software
   distributed under the License is distributed on an "AS IS" BASIS,
   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
   See the License for the specific language governing permissions and
   limitations under the License.
*/

/**
 * This class provides barcode reader for Honeywell devices using Data Collection Intent API
 */

var exec = require('cordova/exec');

var HoneywellBCR = function() {};

/**
 * Constants for checking HoneywellReceiver BCR states
 */

HoneywellBCR.prototype.STATE_NONE = 0;       // we're doing nothing
HoneywellBCR.prototype.STATE_READY = 1; //reading BCR reader
HoneywellBCR.prototype.STATE_READING = 2; //reading BCR reader
HoneywellBCR.prototype.STATE_READ = 3; ///read received BCR reader
HoneywellBCR.prototype.STATE_ERROR = 4; // error
HoneywellBCR.prototype.STATE_DESTROYED = 5; // BCR reader destroyed


/**
 * int HoneywellReceiver bcr
 *
 * @param successCallback function to be called when plugin is init
 * @param errorCallback well never be called
 */
HoneywellBCR.prototype.init = function(successCallback,failureCallback) {
	cordova.exec(successCallback, failureCallback, 'HoneywellBCR', 'init');
};

/**
 * destroy HoneywellReceiver bcr
 * 
 * @param successCallback function to be called when plugin is destroyed
 * @param errorCallback well never be called
 */
HoneywellBCR.prototype.destroy = function(successCallback,failureCallback) {
	cordova.exec(successCallback, failureCallback, 'HoneywellBCR', 'destroy', []);	
};

/**
 * Check HoneywellReceiver bcr current state
 * 
 * @param successCallback(object) returns json object containing state, property state (int)
 * @param errorCallback function to be called when problem fetching state.
 *  
 */
HoneywellBCR.prototype.getState = function(successCallback,failureCallback) {
	cordova.exec(successCallback, failureCallback, 'HoneywellBCR', 'getState', []);
};

/**
 * Read BCR
 * 
 * @param successCallback(data) asynchronous function to be called each time reading was successful.
 * 		returns ASCII string with received data 
 * @param errorCallback asynchronous function to be called when there was a problem while reading
 */
HoneywellBCR.prototype.read = function(successCallback,failureCallback) {
	 cordova.exec(successCallback, failureCallback, 'HoneywellBCR', 'read', []);
};

var honeywellBCR = new HoneywellBCR();
module.exports = honeywellBCR;
