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
      const device = await navigator.bluetooth.requestDevice(
        {
            filters: [{
                services:[ 'heart_rate' ]
            }],
            optionalServices: [PMD_SERVICE, "heart_rate"]
        });
      const server = await device.gatt.connect();
        // configure standard heart rate belt measurement, heart rate, R-R data etc
      const service = await server.getPrimaryService("heart_rate");
      const characteristic = service.getCharacteristic("heart_rate_measurement");
      characteristic.startNotifications().then(c => {
        c.addEventListener('characteristicvaluechanged', hrHandler);
      });

      const ecgService = await server.getPrimaryService(PMD_SERVICE)
        .catch((err) => { errorHandler("No ECG available"); });
      var ecgCtrlCharacteristic = await ecgService.getCharacteristic(PMD_CONTROL);
        // the bit flag array to configure what data to fetch,
        // values drawn from Polar Android Java/Kotlin library
      await ecgCtrlCharacteristic.writeValue(new Uint8Array([0x02, 0, 0x00, 0x01, 0x82, 0x00, 0x01, 0x01, 0x0E, 0x00]));
      console.log("ECG requested");
      const dataCharacteristic = awit service.getCharacteristic(PMD_DATA);
    }
  }

  window.heartRateSensor = new HeartRateSensor();

})();
