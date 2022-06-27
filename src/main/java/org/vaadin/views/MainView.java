package org.vaadin.views;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.LinkedList;

/**
 *
 */
@Route
@CssImport(value = "./grid-styles.css",
        themeFor = "vaadin-grid")
public class MainView extends VerticalLayout {

    Button connectButton = new Button("Connect to a Bluetooth HR belt",
            event -> connect());
    private GridListDataView<RRInterval> rrListDataView;
    private Grid<RRInterval> grid;
    private Checkbox scrollToEnd;

    public MainView() {
        injectWebBluetoothExampleJavaScriptCode();
        add(connectButton);
    }

    H1 heading = new H1("Vaadin + Web Bluetooth example using Polar H10");

    final ListSeries listSeries = new ListSeries();
    //final DataSeries series = new DataSeries();
    ArrayList<RRInterval> values = new ArrayList<>();

    public record RRInterval(int index, int duration, Double variance, Double std, Double relToAvg) {}

    private void buildReportingUI() {
        add(heading);

        final Chart chart = new Chart();

        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("ECG");

//        XAxis xAxis = configuration.getxAxis();
  //      xAxis.setType(AxisType.DATETIME);
    //    xAxis.setTickPixelInterval(150);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("µV"));

        configuration.getTooltip().setEnabled(false);
        configuration.getLegend().setEnabled(false);

//        series.setPlotOptions(new PlotOptionsSpline());
//        series.setName("HR");

//        configuration.setSeries(series);
        configuration.setSeries(listSeries);

        add(chart);

        scrollToEnd = new Checkbox("Autoscroll");

        grid = new Grid<>();
        grid.addColumn( d -> d.index).setHeader("beat index");
        grid.addColumn( d -> d.duration).setHeader("ms");
        grid.addColumn( d -> String.format("%.2f",d.relToAvg)).setHeader("% of avg of last 5 beats");
        grid.addColumn( d -> String.format("%.2f",d.variance)).setHeader("Variance (last 5)");
        grid.addColumn( d -> String.format("%.2f",d.std)).setHeader("Standard deviation (last 5)");
        grid.setClassNameGenerator(rr -> {
            if(rr.relToAvg != null) {
                double relativeChangePercentage = Math.abs(rr.relToAvg - 100);
                if(relativeChangePercentage > 20) {
                    // most likely "missed a beat" or extra beat
                    return "alert";
                } else if(relativeChangePercentage > 10) {
                    // Some other large variance in r-r, most likely just signs of good fit
                    return "warning";
                }
            }
            return "";
        });
        rrListDataView = grid.setItems(new RRInterval[]{});

        add(new H3("Raw R-R data"));
        add(scrollToEnd);
        add(grid);

    }

    LinkedList<Number> samples = new LinkedList<>();
    LocalDateTime lastPlot = LocalDateTime.now();

    @ClientCallable
    public void handleECGData(String base64encoded) {
        // HEX: 00 38 6C 31 72 A4 D3 23 0D 03 FF
        // index    type                                data
        // 0:      Measurement type                     00 (Ecg data)
        // 1..8:   64-bit Timestamp                     38 6C 31 72 A4 D3 23 0D (0x0D23D3A472316C38 = 946833049921875000)
        // 9:      Frame type                           03 (raw, frame type 3)
        // 10:     Data                                 FF
        byte[] bytes = Base64.getDecoder().decode(base64encoded);
        int datatype = bytes[0];
        LocalDateTime localDateTime = LocalDateTime.ofEpochSecond(convertArrayToUnsignedLong(bytes, 1, 8)/1000, 0, ZoneOffset.UTC);
        System.out.println("Timestamp" + localDateTime);
        int frametype = bytes[9];
        System.out.println("Frametype" + frametype);
        byte[] measurements = Arrays.copyOfRange(bytes, 10, bytes.length);
        int offset = 0;

        while(offset < measurements.length) {
            int microVolts = convertArrayToSignedInt(measurements, offset, 3);
            System.out.print(microVolts + ",");
            samples.add(microVolts);
            offset += 3;
        }
        if(lastPlot.plusSeconds(3).isBefore(LocalDateTime.now())) {
            listSeries.setData(samples);
            listSeries.updateSeries();
            samples = new LinkedList<>();
            lastPlot = LocalDateTime.now();
        }
        System.out.println();
    }

    // Polar BleUtils
    public static int convertArrayToSignedInt(byte[] data, int offset, int length) {
        int result = (int) convertArrayToUnsignedLong(data, offset, length);
        if ((data[offset + length - 1] & 0x80) != 0) {
            int mask = 0xFFFFFFFF << length * 8;
            result |= mask;
        }
        return result;
    }

    // Polar BleUtils
    public static long convertArrayToUnsignedLong(byte[] data, int offset, int length) {
        long result = 0;
        for (int i = 0; i < length; ++i) {
            result |= (((long) data[i + offset] & 0xFFL) << i * 8);
        }
        return result;
    }

/*
    private final void dataFromRawType0(byte[] value, long timeStamp) {
        EcgData ecgData = new EcgData(timeStamp);
        int offset = 0;

        while(offset < value.length) {
            int microVolts = BleUtils.convertArrayToSignedInt(value, offset, 3);
            offset += 3;
            ecgData.ecgSamples.add(new EcgData.EcgSample(timeStamp, microVolts, false, (byte)0, (byte)0, (byte)0, (byte)0, 124, (DefaultConstructorMarker)null));
        }

        return ecgData;
    }
    private final void dataFromRawType1(byte[] value, long timeStamp) {
        EcgData ecgData = new EcgData(timeStamp);
        int offset = 0;

        while(offset < value.length) {
            int microVolts = (value[offset] & 255 | (value[offset + 1] & 63) << 8) & 16383;
            boolean overSampling = (value[offset + 2] & 1) != 0;
            byte skinContactBit = (byte)((value[offset + 2] & 6) >> 1);
            byte contactImpedance = (byte)((value[offset + 2] & 24) >> 3);
            offset += 3;
            ecgData.ecgSamples.add(new EcgData.EcgSample(timeStamp, microVolts, overSampling, skinContactBit, contactImpedance, (byte)0, (byte)0, 96, (DefaultConstructorMarker)null));
        }

        return ecgData;
    }

    private final void dataFromRawType2(byte[] value, long timeStamp) {
        EcgData ecgData = new EcgData(timeStamp);
        int offset = 0;

        while(offset < value.length) {
            int microVolts = (value[offset] & 255 | (value[offset + 1] & 255) << 8 | (value[offset + 2] & 3) << 16) & 4194303;
            byte ecgDataTag = (byte)((value[offset + 2] & 28) >> 2);
            byte paceDataTag = (byte)((value[offset + 2] & 224) >> 5);
            offset += 3;
            ecgData.ecgSamples.add(new EcgData.EcgSample(timeStamp, microVolts, false, (byte)0, (byte)0, ecgDataTag, paceDataTag, 28, (DefaultConstructorMarker)null));
        }

        return ecgData;
    }

    */


    @ClientCallable
    public void handleHeartRateData(JsonObject json) {
        double heartRate = json.getNumber("heartRate");
        heading.setText("Current heart rate: " + heartRate);

//        boolean shift = series.size() > 50;
//        series.add(new DataSeriesItem(Instant.now(), heartRate), true, shift);

        JsonArray rrIntervals = json.getArray("rrIntervals");
        if(rrIntervals != null) {
            for (int i = 0; i < rrIntervals.length(); i++) {
                double number = rrIntervals.getNumber(i);
                handleRRInterval(number);
            }
        }
    }

    private void handleRRInterval(double duration) {
        Double v = null;
        Double std = null;
        Double relToAvg = null;
        if(values.size() > 5) {
            // calculate variance

            int[] data = new int[5];
            data[4] = (int) duration;
            for(int i = 0; i < 4; i++) {
                data[i] = values.get(values.size() - i -1).duration;
            }

            // The mean average
            double mean = 0.0;
            for (int i = 0; i < data.length; i++) {
                mean += data[i];
            }
            mean /= data.length;

            relToAvg = 100*duration / mean;

            // The variance
            double variance = 0;
            for (int i = 0; i < data.length; i++) {
                variance += Math.pow(data[i] - mean, 2);
            }
            variance /= data.length;

            // Standard Deviation
            std = Math.sqrt(variance);
            v = variance;
        }
        RRInterval rrInterval = new RRInterval(rrListDataView.getItemCount(), (int) duration, v, std, relToAvg);
        values.add(rrInterval);
        rrListDataView.addItem(rrInterval);
        if(scrollToEnd.getValue()) {
            grid.scrollToEnd();
        }
    }

    public void connect() {
        getElement().executeJs("""
                var el = this;
                  window.heartRateSensor.connect()
                  .then(() => heartRateSensor.startNotificationsHeartRateMeasurement().then(
                    function handleHeartRateMeasurement(heartRateMeasurement) {
                        heartRateMeasurement.addEventListener('characteristicvaluechanged', event => {
                            el.$server.handleECGData(btoa(String.fromCharCode(...new Uint8Array(event.target.value.buffer))))
                        })
                    }))
                  .catch(error => {
                    window.alert("It doesn't works!");
                    console.error(error);
                  });
                """);
        remove(connectButton);
        buildReportingUI();
    }

    private void injectWebBluetoothExampleJavaScriptCode() {
        // https://webbluetoothcg.github.io/demos/heart-rate-sensor/
        UI.getCurrent().getPage().executeJs("""
                (function() {
                  'use strict';
                    const PMD_SERVICE = "fb005c80-02e7-f387-1cad-8acd2d8df0c8";
                    const PMD_CONTROL = "fb005c81-02e7-f387-1cad-8acd2d8df0c8";
                    const PMD_DATA =    "fb005c82-02e7-f387-1cad-8acd2d8df0c8";

                  class HeartRateSensor {
                    constructor() {
                      this.device = null;
                      this.server = null;
                      this._characteristics = new Map();
                    }

                    connect() {
                      return navigator.bluetooth.requestDevice(
                        {
                            filters: [{ namePrefix: "Polar H10"}],
                            acceptAllDevices: false,
                            manufacturerData: [{ companyIdentifier: 0x00D1 }],
                            optionalServices: [PMD_SERVICE]
                        })
                      .then(device => {
                        this.device = device;
                        return device.gatt.connect();
                      })
                      .then(server => {
                        this.server = server;
                        /*
                        server.getPrimaryService(PMD_SERVICE).then(service => {
                            service.getCharacteristic(PMD_CONTROL).then( character => {
                                character.writeValue(new Uint8Array([0x02, 0, 0x00, 0x01, 0x82, 0x00, 0x01, 0x01, 0x0E, 0x00])).then(c => {
                                    character.startNotifications().then( m => {
                                        m.addEventListener('characteristicvaluechanged', event => {
                                            console.log("WWW");
                                            debugger;
                                            console.log(event);
                                        });
                                    });
                        
                                    service.getCharacteristic(PMD_DATA).then( data => {
                                        debugger;
                                        console.log(data);
                                    });
                                });
                            });
                        });
                        */
                        return server.getPrimaryService(PMD_SERVICE);
                      })
                      .then(service => {
                          service.getCharacteristic(PMD_CONTROL).then( character => {
                                character.writeValue(new Uint8Array([0x02, 0, 0x00, 0x01, 0x82, 0x00, 0x01, 0x01, 0x0E, 0x00])).then(c => {
                                    console.log("ECG requested");
                                });
                            });
                      
                        return this._cacheCharacteristic(service, PMD_DATA);
                      })
                    }

                    /* Heart Rate Service */

                    startNotificationsHeartRateMeasurement() {
                      return this._startNotifications(PMD_DATA);
                    }
                    stopNotificationsHeartRateMeasurement() {
                      return this._stopNotifications('PMD_DATA');
                    }
                    parseHeartRate(value) {
                      // In Chrome 50+, a DataView is returned instead of an ArrayBuffer.
                      console.log(value);
                      value = value.buffer ? value : new DataView(value);
                      let flags = value.getUint8(0);
                      let rate16Bits = flags & 0x1;
                      let result = {};
                      let index = 1;
                      if (rate16Bits) {
                        result.heartRate = value.getUint16(index, /*littleEndian=*/true);
                        index += 2;
                      } else {
                        result.heartRate = value.getUint8(index);
                        index += 1;
                      }
                      let contactDetected = flags & 0x2;
                      let contactSensorPresent = flags & 0x4;
                      if (contactSensorPresent) {
                        result.contactDetected = !!contactDetected;
                      }
                      let energyPresent = flags & 0x8;
                      if (energyPresent) {
                        result.energyExpended = value.getUint16(index, /*littleEndian=*/true);
                        index += 2;
                      }
                      let rrIntervalPresent = flags & 0x10;
                      if (rrIntervalPresent) {
                        let rrIntervals = [];
                        for (; index + 1 < value.byteLength; index += 2) {
                          rrIntervals.push(value.getUint16(index, /*littleEndian=*/true));
                        }
                        result.rrIntervals = rrIntervals;
                      }
                      return result;
                    }

                    /* Utils */
                    
                    _cacheCharacteristic(service, characteristicUuid) {
                      return service.getCharacteristic(characteristicUuid)
                      .then(characteristic => {
                        this._characteristics.set(characteristicUuid, characteristic);
                      });
                    }
                    _readCharacteristicValue(characteristicUuid) {
                      let characteristic = this._characteristics.get(characteristicUuid);
                      return characteristic.readValue()
                      .then(value => {
                        // In Chrome 50+, a DataView is returned instead of an ArrayBuffer.
                        value = value.buffer ? value : new DataView(value);
                        return value;
                      });
                    }
                    _writeCharacteristicValue(characteristicUuid, value) {
                      let characteristic = this._characteristics.get(characteristicUuid);
                      return characteristic.writeValue(value);
                    }
                    _startNotifications(characteristicUuid) {
                      let characteristic = this._characteristics.get(characteristicUuid);
                      // Returns characteristic to set up characteristicvaluechanged event
                      // handlers in the resolved promise.
                      return characteristic.startNotifications()
                      .then(() => characteristic);
                    }
                    _stopNotifications(characteristicUuid) {
                      let characteristic = this._characteristics.get(characteristicUuid);
                      // Returns characteristic to remove characteristicvaluechanged event
                      // handlers in the resolved promise.
                      return characteristic.stopNotifications()
                      .then(() => characteristic);
                    }
                  }

                  window.heartRateSensor = new HeartRateSensor();

                })();

                                """);
    }

}

