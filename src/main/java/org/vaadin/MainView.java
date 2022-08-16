package org.vaadin;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.*;
import com.vaadin.flow.component.dependency.CssImport;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.html.VDiv;
import org.vaadin.firitin.components.html.VH5;
import org.vaadin.firitin.components.html.VParagaph;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.fields.internalhtmltable.Table;
import org.vaadin.firitin.fields.internalhtmltable.TableRow;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Base64;
import java.util.LinkedList;
import java.util.TimeZone;

@Route
// Inject a small Javascript to connect and configure
// a Bluetooth low energy heart rate belt
@JavaScript(value = "./h10tooling.js", loadMode = LoadMode.EAGER)
@CssImport(value = "./grid-styles.css", themeFor = "vaadin-grid")
public class MainView extends VerticalLayout {

    private Button connectButton = new Button("Connect to a Bluetooth HR belt",
            event -> connect());
    private Table hrvTable;
    private Chart ecgChart;

    public MainView() {
        add(new RichText().withMarkDownResource("/app-readme.md"));
        // Initially just show one button to connect to Web Bluetooth device
        add(connectButton);
    }

    private Span time = new Span();
    private Span hr = new Span();
    private Span sd = new Span();
    private Span hrv = new Span();

    private ListSeries listSeries = new ListSeries();
    private DataSeries hrDataSeries = new DataSeries();

    private void buildReportingUI() {

        ecgChart = new Chart();
        ecgChart.setHeight("50vh");

        Configuration configuration = ecgChart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);

        YAxis yAxis = configuration.getyAxis();
        yAxis.setTitle(new AxisTitle("ECG µV, last ~ 5 seconds"));

        configuration.getTooltip().setEnabled(false);
        configuration.getLegend().setEnabled(false);
        configuration.setSeries(listSeries);

        add(ecgChart);

        final Chart hrChart = new Chart();
        hrChart.setHeight("40vh");

        configuration = hrChart.getConfiguration();
        configuration.getChart().setType(ChartType.SPLINE);

        XAxis xAxis = configuration.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);
        configuration.getyAxis().setTitle("Heart rate (bpm)");

        PlotOptionsSpline plotOptionsSpline = new PlotOptionsSpline();
        plotOptionsSpline.setMarker(new Marker(false));
        hrDataSeries.setPlotOptions(plotOptionsSpline);
        hrDataSeries.setName("HR");
        configuration.setSeries(hrDataSeries);

        VDiv currentStatus = new VDiv();
        currentStatus.setClassName("nowrap");
        currentStatus.add(new H3("Heart rate"));
        currentStatus.add(new VH5("Current: ").withComponents(hr));
        currentStatus.add(new VH5("Variation:"));
        currentStatus.add(new Emphasis("~ stress level, bigger -> less stress"));
        currentStatus.add(new VParagaph("RMSSD 10: "){{setTitle("RMSSD. This is the Root Mean Square of Successive Differences between each heartbeat. Commonly used in the science to analyse heart rate variability. Here calculated for the last 10 beats.");}}.withComponents(hrv));
        currentStatus.add(new VParagaph("SD 5: "){{setTitle("Standard deviation of last 5 R-R intervals");}}.withComponents(sd));
        currentStatus.add(new VH5("Time: ").withComponents(time));

        hrvTable = new Table();
        TableRow headerRow = hrvTable.getHead().addRow();
        headerRow.addHeaderCell().setText("idx");
        headerRow.addHeaderCell().setText("ms");
        headerRow.addHeaderCell().setText("diff");
        headerRow.addHeaderCell().setText("SD (last 5)");
        headerRow.addHeaderCell().setText("HRV (10)");

        add(new VHorizontalLayout().withExpanded(
                currentStatus,
                hrChart,
                new VVerticalLayout(
                        new H5("RR-data"),
                        hrvTable
                )
        ));

    }

    LinkedList<Number> samples = new LinkedList<>();
    LocalDateTime lastPlot = LocalDateTime.now();

    HeartRateVariabilityAnalysis hrvAnalysis = new HeartRateVariabilityAnalysis();

    /**
     * A hook for the minimal JS hooks to send the ECG data
     * @param base64encoded raw ECG data bytes as base64 encoded string
     */
    @ClientCallable
    public void handleECGData(String base64encoded) {
        // Vaadin and many other web frameworks don't support
        // transferring raw byte data, decode the Base64 encoded byte array
        byte[] bytes = Base64.getDecoder().decode(base64encoded);
        // Now we can actually parse the raw byte array into
        // Java data structure
        EcgData ecgData = PolarH10Tooling.parseEcgData(bytes);

        // Collect samples until 5 seconds has passed,
        // then update the chart in the UI with new batch
        samples.addAll(ecgData.samplesAsList());

        if(lastPlot.isBefore(LocalDateTime.now().minusSeconds(5))) {
            listSeries.setData(samples);
            listSeries.updateSeries();

            samples = new LinkedList<>();
            lastPlot = LocalDateTime.now();
        }
    }

    /**
     * A hook for the minimal JS hooks to send the heart rate data
     * @param base64encoded raw data bytes as base64 encoded string
     */
    @ClientCallable
    public void handleHeartRateData(String base64encoded) {
        byte[] bytes = Base64.getDecoder().decode(base64encoded);
        HrmData hrmData = PolarH10Tooling.parseHeartRateBeltData(bytes);

        time.setText(LocalTime.now().toString());
        hr.setText(hrmData.heartRate()+"");

        boolean shift = hrDataSeries.size() > 50;
        hrDataSeries.add(new DataSeriesItem(Instant.now(), hrmData.heartRate()), true, shift);
        if(hrmData.rrIntervals() != null) {
            for (int i : hrmData.rrIntervals()) {
                HeartRateVariabilityAnalysis.RRInterval rr = hrvAnalysis.logRRInterval(i);
                TableRow r = hrvTable.getBody().insertRow(0);
                r.addDataCell().setText(rr.index()+"");
                r.addDataCell().setText(rr.duration()+"");
                r.addDataCell().setText(rr.diff()+"");
                r.addDataCell().setText(String.format("%.2f",rr.std()));
                r.addDataCell().setText(String.format("%.2f",rr.rms10()));

                if(rr.relToAvg() != null) {
                    // highlight rows if something weird is happening,
                    // extra beats etc
                    double relativeChangePercentage = Math.abs(rr.relToAvg() - 100);
                    if(relativeChangePercentage > 20) {
                        // most likely "missed a beat" or extra beat
                        r.setClassName("alert");
                    } else if(relativeChangePercentage > 10) {
                        // Some other large variance in r-r, most likely just signs of good fit
                        r.setClassName("warning");
                    }
                }

                final int MAX_ROWS = 200;
                if(hrvTable.getBody().getRows().size() > MAX_ROWS) {
                    hrvTable.getBody().removeRow(MAX_ROWS);
                }

                sd.setText(String.format("%.2f",rr.std()));
                hrv.setText(String.format("%.2f",rr.rms10()));
            }
        }

    }

    @ClientCallable
    private void handleError(String errorMsg) {
        if(errorMsg.contains("No ECG")) {
            ecgChart.setVisible(false);
            Notification.show("""
                ECG data from your heart rate monitor is not 
                available or not supported. 
                Try with Polar H10 for all features.
            """);
        } else {
            Notification.show(errorMsg);
        }
    }

    private void connect() {
        removeAll();
        buildReportingUI();

        getElement().executeJs("""
                var el = this;
                
                function ecgHandler(event) {
                    // get the raw date from event, Base64 encode to send to
                    // Vaadin server side (that accepts String for the communication)
                    el.$server.handleECGData(btoa(String.fromCharCode(...new Uint8Array(event.target.value.buffer))));
                }
                function hrmHandler(event) {
                    el.$server.handleHeartRateData(btoa(String.fromCharCode(...new Uint8Array(event.target.value.buffer))));
                }
                function errorHandler(errorMsg) {
                    el.$server.handleError(errorMsg);
                }
                // heartRateSensor object created by h10tooling.js
                window.connectHrMonitor(ecgHandler, hrmHandler, errorHandler);
        """);
    }

}

