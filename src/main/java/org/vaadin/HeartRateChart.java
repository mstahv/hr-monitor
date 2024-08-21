package org.vaadin;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisType;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.DataSeries;
import com.vaadin.flow.component.charts.model.DataSeriesItem;
import com.vaadin.flow.component.charts.model.Marker;
import com.vaadin.flow.component.charts.model.PlotOptionsSpline;
import com.vaadin.flow.component.charts.model.Time;
import com.vaadin.flow.component.charts.model.XAxis;

import java.time.Instant;
import java.util.TimeZone;

public class HeartRateChart extends Chart {
    public static final int MAX_SAMPLES = 50;
    private DataSeries hrDataSeries = new DataSeries();

    public HeartRateChart() {
        super(ChartType.SPLINE);
        setHeight("40vh");

        var c = getConfiguration();

        XAxis xAxis = c.getxAxis();
        xAxis.setType(AxisType.DATETIME);
        xAxis.setTickPixelInterval(150);

        c.getyAxis().setTitle("Heart rate (bpm)");

        PlotOptionsSpline plotOptionsSpline = new PlotOptionsSpline();
        plotOptionsSpline.setMarker(new Marker(false));
        hrDataSeries.setPlotOptions(plotOptionsSpline);
        hrDataSeries.setName("HR");
        c.getLegend().setEnabled(false);
        c.setSeries(hrDataSeries);
        Time time = new Time();
        // Yes, HC/VC wants minututes, positive towards west 🫣
        time.setTimezoneOffset(-TimeZone.getDefault().getOffset(System.currentTimeMillis())/ (60*1000));
        c.setTime(time);
    }

    public void addHeartRate(int heartRate) {
        boolean shift = hrDataSeries.size() > MAX_SAMPLES;
        hrDataSeries.add(
                new DataSeriesItem(
                    Instant.now(),
                    heartRate),
                true, // redraw immediately
                shift // remove previous
        );
    }
}
