/*
 * JS code derived from https://webbluetoothcg.github.io/demos/heart-rate-sensor/
 * and Polar Ble
 */
(function() {
  'use strict';
    // Constants for Polar H10 heart rate belt
    const PMD_SERVICE = "fb005c80-02e7-f387-1cad-8acd2d8df0c8";
    const PMD_CONTROL = "fb005c81-02e7-f387-1cad-8acd2d8df0c8";
    const PMD_DATA =    "fb005c82-02e7-f387-1cad-8acd2d8df0c8";

  class HeartRateSensor {
    constructor() {}

    connect(ecgHandler, hrHandler, errorHandler) {
      return navigator.bluetooth.requestDevice(
        {
            filters: [{
                services:[ 'heart_rate' ]
            }],
//            acceptAllDevices: false,
//            manufacturerData: [{ companyIdentifier: 0x00D1 }],
            optionalServices: [PMD_SERVICE, "heart_rate"]
        })
      .then(device => {
        return device.gatt.connect();
      })
      .then(server => {
        // configure standard heart rate belt measurement, heart rate, R-R data etc
        server.getPrimaryService("heart_rate")
        .then(service => {
          service.getCharacteristic("heart_rate_measurement").then(characteristic => {
            characteristic.startNotifications().then(c => {
              c.addEventListener('characteristicvaluechanged', hrHandler);
            });
          });
        });


        // Also use Polar H10 specific ECG data
        server.getPrimaryService(PMD_SERVICE).then(service => {
          service.getCharacteristic(PMD_CONTROL).then(ch => {
            // the bit flag array to configure what data to fetch,
            // values drawn from Polar Android Java/Kotlin library
            ch.writeValue(new Uint8Array([0x02, 0, 0x00, 0x01, 0x82, 0x00, 0x01, 0x01, 0x0E, 0x00])).then(c => {
              console.log("ECG requested");
              service.getCharacteristic(PMD_DATA).then(c => {
                c.startNotifications().then(c => {
                  c.addEventListener('characteristicvaluechanged', ecgHandler);
                });
              });
            });
          });
        }).catch(error => {errorHandler("No ECG available"); });
      });
    }    
  }

  window.heartRateSensor = new HeartRateSensor();

})();
