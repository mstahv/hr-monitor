package org.vaadin.views;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.UI;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.checkbox.Checkbox;
import com.vaadin.flow.component.grid.Grid;
import com.vaadin.flow.component.grid.dataview.GridListDataView;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Element;
import com.vaadin.flow.router.Route;
import elemental.json.JsonArray;
import elemental.json.JsonObject;

import java.time.Instant;
import java.util.ArrayList;

/**
 * The main view is a top-level placeholder for other views.
 */
@Route
public class MainView extends VerticalLayout {

    Button button = new Button("Connect to a Bluetooth HR belt",
            event -> connect());
    private GridListDataView<RRInterval> rrListDataView;
    private Grid<RRInterval> grid;
    private Checkbox scrollToEnd;

    public MainView() {

        injectWebBluetoothExampleCode();

        add(button);
    }

    public void connect() {
        getElement().executeJs("""
                var el = this;
                debugger;
                  window.heartRateSensor.connect()
                  .then(() => heartRateSensor.startNotificationsHeartRateMeasurement().then(
                    function handleHeartRateMeasurement(heartRateMeasurement) {
                        heartRateMeasurement.addEventListener('characteristicvaluechanged', event => {
                            var data = window.heartRateSensor.parseHeartRate(event.target.value);
                            console.log("HR measurement: " +  data.heartRate);
                            el.$server.handleHeartRateData(data);
                        })
                    }))
                  .catch(error => {
                    window.alert("It doesn't works!");
                    console.error(error);
                  });
                """);
        remove(button);
        buildReportingUI();
    }

    H1 heading = new H1("Heart rate: wait for it.... ");

    final DataSeries series = new DataSeries();
    ArrayList<RRInterval> values = new ArrayList<>();

    public record RRInterval(int index, int duration, Double variance, Double std, Double relToAvg) {}

    private void buildReportingUI() {

        Element styleElement = new Element("style");
        styleElement.setText("""
                .warning {color:yellow;}
                .error {color:red;}
        """);
        getElement().appendChild(styleElement);

        add(heading);

        final Chart chart = new Chart();

        final Configuration configuration = chart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);
        configuration.getTitle().setText("Heart rate");

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("Value"));

        configuration.getTooltip().setEnabled(false);
        configuration.getLegend().setEnabled(false);

        series.setPlotOptions(new PlotOptionsSpline());
        series.setName("HR");

        configuration.setSeries(series);

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
                if(relativeChangePercentage > 10) {
                    return "warning";
                } else if(relativeChangePercentage > 20) {
                    // most likely "missed a beat" or extra beat
                    return "alert";
                }
            }
            return "";
        });
        rrListDataView = grid.setItems(new RRInterval[]{});

        add(new H3("Raw R-R data"));
        add(scrollToEnd);
        add(grid);

    }

    @ClientCallable
    public void handleHeartRateData(JsonObject json) {
        double heartRate = json.getNumber("heartRate");
        heading.setText("Current heart rate: " + heartRate);

        boolean shift = series.size() > 50;
        series.add(new DataSeriesItem(Instant.now(), heartRate), true, shift);

        JsonArray rrIntervals = json.getArray("rrIntervals");
        if(rrIntervals != null) {
            for (int i = 0; i < rrIntervals.length(); i++) {
                double number = rrIntervals.getNumber(i);
                System.out.println(number + " / " + i );
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

    private void injectWebBluetoothExampleCode() {
        UI.getCurrent().getPage().executeJs("""
                (function() {
                  'use strict';

                  class HeartRateSensor {
                    constructor() {
                      this.device = null;
                      this.server = null;
                      this._characteristics = new Map();
                    }
                    connect() {
                      return navigator.bluetooth.requestDevice({filters:[{services:[ 'heart_rate' ]}]})
                      .then(device => {
                        this.device = device;
                        return device.gatt.connect();
                      })
                      .then(server => {
                        this.server = server;
                        return server.getPrimaryService('heart_rate');
                      })
                      .then(service => {
                        return this._cacheCharacteristic(service, 'heart_rate_measurement');
                      })
                    }

                    /* Heart Rate Service */

                    startNotificationsHeartRateMeasurement() {
                      return this._startNotifications('heart_rate_measurement');
                    }
                    stopNotificationsHeartRateMeasurement() {
                      return this._stopNotifications('heart_rate_measurement');
                    }
                    parseHeartRate(value) {
                      // In Chrome 50+, a DataView is returned instead of an ArrayBuffer.
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

