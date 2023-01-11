# zapplica-plugin-honeywellbcr
Cordova/Phonegap plugin to receive data from honeywell scanner using Data Collection Intent API

You need listen yourself to the cordova pause and resume events and run respectivly the init and destroy functions to claim and release the scanner.

The read function will launch success callback each time a barcode is received.