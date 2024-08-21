/*
 * JS code inspired by https://webbluetoothcg.github.io/demos/heart-rate-sensor/
 * and Polar Ble SDK for Android.
 * Connects to any heart rate monitor and especially Polar H10 ECG belt.
 * Handlers republish the data coming from heart rate belt as custom events on
 * the document body, DataView base 64 encoded:
 *
 * hrm-heart-rate: heart rate data
 * hrm-ecg-data: ECG data
 * hrm-error: error message
 *
 * In this example the data is
 * transferred to the Java side with Vaadin for further processing.
 */
(function() {

    // base64 encoded DataView
    const b64 = b => btoa(String.fromCharCode(...new Uint8Array(b.buffer)));

    // Constants for Polar H10 heart rate belt
    const PMD_SERVICE = "fb005c80-02e7-f387-1cad-8acd2d8df0c8";
    const PMD_CONTROL = "fb005c81-02e7-f387-1cad-8acd2d8df0c8";
    const PMD_DATA =    "fb005c82-02e7-f387-1cad-8acd2d8df0c8";

    async function connectHrMonitor() {
      const device = await navigator.bluetooth.requestDevice(
        {
            filters: [{ services:[ 'heart_rate' ] }],
            optionalServices: [PMD_SERVICE]
        });
      const server = await device.gatt.connect();
      // configure standard heart rate belt measurement, heart rate, R-R data etc
      // This works on most HR belts, somewhat standardized data format
      const service = await server.getPrimaryService("heart_rate");
      const characteristic = await service.getCharacteristic("heart_rate_measurement");
      await characteristic.startNotifications();
      characteristic.addEventListener('characteristicvaluechanged', e => {
        document.body.dispatchEvent(new CustomEvent('hrm-heart-rate', {detail: b64(e.target.value)}));
      });
      // Try to read ECG data from Polar H10 belt
      const ecgService = await server.getPrimaryService(PMD_SERVICE)
        .catch((err) => { document.body.dispatchEvent(new CustomEvent("hrm-error", {detail: "No ECG available"})) });;
      var ecgCtrlCharacteristic = await ecgService.getCharacteristic(PMD_CONTROL);
      // The bit flag array to configure belt to send ECG data,
      // values drawn from Polar Android Java/Kotlin library
      await ecgCtrlCharacteristic.writeValue(new Uint8Array([0x02, 0, 0x00, 0x01, 0x82, 0x00, 0x01, 0x01, 0x0E, 0x00]));
      console.log("ECG requested");

      const dataCharacteristic = await ecgService.getCharacteristic(PMD_DATA);
      await dataCharacteristic.startNotifications();
      dataCharacteristic.addEventListener('characteristicvaluechanged', e => {
        document.body.dispatchEvent(new CustomEvent('hrm-ecg-data', {detail: b64(e.target.value)}));
      });
    }
    window.connectHrMonitor = connectHrMonitor;
})();
