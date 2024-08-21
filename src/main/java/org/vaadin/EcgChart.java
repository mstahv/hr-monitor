package org.vaadin;

import com.vaadin.flow.component.charts.Chart;
import com.vaadin.flow.component.charts.model.AxisTitle;
import com.vaadin.flow.component.charts.model.ChartType;
import com.vaadin.flow.component.charts.model.ListSeries;
import com.vaadin.flow.component.charts.model.YAxis;

import java.util.LinkedList;

public class EcgChart extends Chart {
    private final ListSeries listSeries = new ListSeries();

    public EcgChart() {
        super(ChartType.SPLINE);
        setHeight("50vh");

        var c = getConfiguration();
        YAxis yAxis = c.getyAxis();
        yAxis.setTitle(new AxisTitle("ECG µV, last ~ 5 seconds"));

        c.getTooltip().setEnabled(false);
        c.getLegend().setEnabled(false);
        c.setSeries(listSeries);
    }

    public void setSamples(LinkedList<Number> samples) {
        listSeries.setData(samples);
        listSeries.updateSeries();
    }
}
