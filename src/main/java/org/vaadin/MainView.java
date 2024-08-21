package org.vaadin;

import com.vaadin.flow.component.ClientCallable;
import com.vaadin.flow.component.button.Button;
import com.vaadin.flow.component.dependency.JavaScript;
import com.vaadin.flow.component.dependency.StyleSheet;
import com.vaadin.flow.component.html.Emphasis;
import com.vaadin.flow.component.html.H2;
import com.vaadin.flow.component.html.H5;
import com.vaadin.flow.component.html.Span;
import com.vaadin.flow.component.notification.Notification;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.dom.Style;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.shared.ui.LoadMode;
import org.vaadin.addons.velocitycomponent.VElement;
import org.vaadin.firitin.components.RichText;
import org.vaadin.firitin.components.html.VDiv;
import org.vaadin.firitin.components.html.VH5;
import org.vaadin.firitin.components.html.VParagaph;
import org.vaadin.firitin.components.orderedlayout.VHorizontalLayout;
import org.vaadin.firitin.components.orderedlayout.VVerticalLayout;
import org.vaadin.firitin.fields.internalhtmltable.TableRow;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.LinkedList;

@Route
// Inject a small Javascript to connect and configure
// a Bluetooth low energy heart rate belt
@JavaScript(value = "./h10tooling.js", loadMode = LoadMode.EAGER)
@StyleSheet("./styles.css")
public class MainView extends VerticalLayout {

    private Button connectButton = new Button("Connect to a Bluetooth HR belt",
            event -> connect());
    private RRTable rrTable;
    private EcgChart ecgChart;
    private HeartRateChart hrChart;

    public MainView() {
        add(new RichText().withMarkDownResource("/app-readme.md"));
        // Initially just show one button to connect to Web Bluetooth device
        add(connectButton);
    }

    private Span time = new Span();
    private H2 hr = new H2();
    private Span sd = new Span();
    private Span hrv = new Span();


    private void buildReportingUI() {
        add(ecgChart = new EcgChart());

        VDiv currentStatus = new VDiv();
        currentStatus.getStyle().setWhiteSpace(Style.WhiteSpace.NOWRAP);
        currentStatus.add(hr);
        currentStatus.add(new VH5("Variation:"));
        currentStatus.add(new Emphasis("(~ inverted stress level)"));
        currentStatus.add(new VParagaph("RMSSD 10: "){{setTitle("RMSSD. This is the Root Mean Square of Successive Differences between each heartbeat. Commonly used in the science to analyse heart rate variability. Here calculated for the last 10 beats.");}}.withComponents(hrv));
        currentStatus.add(new VParagaph("SD 5: "){{setTitle("Standard deviation of last 5 R-R intervals");}}.withComponents(sd));
        currentStatus.add(new VH5("Time: ").withComponents(time));

        rrTable = new RRTable();

        add(new VHorizontalLayout()
                .withComponent(currentStatus)
                .withExpanded(hrChart = new HeartRateChart())
                .withComponents(
                    new VVerticalLayout(
                        new H5("RR-data"),
                            rrTable
                    ).withSizeUndefined().withSpacing(false).withPadding(false)
                )
        );
    }

    LinkedList<Number> samples = new LinkedList<>();
    LocalDateTime lastPlot = LocalDateTime.now();

    HeartRateVariabilityAnalysis hrvAnalysis = new HeartRateVariabilityAnalysis();

    private void handleECGData(byte[] bytes) {
        EcgData ecgData = PolarH10Tooling.parseEcgData(bytes);

        // Collect samples until 5 seconds has passed,
        // then update the chart in the UI with new batch
        samples.addAll(ecgData.samplesAsList());

        if(lastPlot.isBefore(LocalDateTime.now().minusSeconds(5))) {
            ecgChart.setSamples(samples);
            samples = new LinkedList<>();
            lastPlot = LocalDateTime.now();
        }
    }

    private void handleHeartRateData(byte[] bytes) {
        var hrmData = PolarH10Tooling.parseHeartRateBeltData(bytes);
        hrChart.addHeartRate(hrmData.heartRate());

        time.setText(LocalTime.now().truncatedTo(ChronoUnit.SECONDS).toString());
        hr.setText("Heart rate: %s bpm".formatted(hrmData.heartRate()));

        if(hrmData.rrIntervals() != null) {
            for (int i : hrmData.rrIntervals()) {
                HeartRateVariabilityAnalysis.RRInterval rr = hrvAnalysis.logRRInterval(i);
                rrTable.addRRInterval(rr);
                sd.setText(String.format("%.2f",rr.std()));
                hrv.setText(String.format("%.2f",rr.rms10()));
            }
        }

    }

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

        getElement().executeJs("window.connectHrMonitor()");

        // Start to listen events from the heart rate monitor
        var bodyElement = VElement.body();
        bodyElement.on("hrm-error", String.class, this::handleError);
        bodyElement.on("hrm-heart-rate", byte[].class, this::handleHeartRateData);
        bodyElement.on("hrm-ecg-data", byte[].class, this::handleECGData);

    }

}

