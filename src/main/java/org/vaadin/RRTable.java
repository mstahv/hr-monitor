package org.vaadin;

import com.vaadin.flow.component.grid.GridVariant;
import org.vaadin.firitin.components.grid.VGrid;
import org.vaadin.firitin.fields.internalhtmltable.TableRow;

import java.util.ArrayList;
import java.util.List;

public class RRTable extends VGrid<HeartRateVariabilityAnalysis.RRInterval> {

    List<HeartRateVariabilityAnalysis.RRInterval> rrIntervals = new ArrayList<>();

    public RRTable() {
        super(HeartRateVariabilityAnalysis.RRInterval.class);
        setMinWidth("350px");
        setHeightFull();
        addThemeVariants(GridVariant.LUMO_COMPACT);
        removeAllColumns();
        addColumn("index")
                .setHeader("idx")
                .setWidth("40px");
        addColumn("duration").setHeader("ms").setWidth("55px");
        addColumn("diff").setHeader("Δ").setWidth("45px");
        addColumn(rr -> String.format("%.2f", rr.std()))
                .setHeader("SD (last 5)");
        addColumn(rr -> String.format("%.2f", rr.rms10()))
                .setHeader("HRV(10");
        ;
        getColumns().forEach(c -> c.setSortable(false));
        withColumnSelector();

        withRowStyler((rr, style) -> {
            if(rr.relToAvg() != null) {
                // highlight rows if something weird is happening, "extra beats" etc
                double relativeChangePercentage = Math.abs(rr.relToAvg() - 100);
                if(relativeChangePercentage > 20) {
                    // most likely "missed a beat" or extra beat
                    style.setBackground("red");
                } else if(relativeChangePercentage > 4) {
                    // Some other large variance in r-r, most likely just signs of good fit
                    style.setBackground("yellow");
                }
            }
        });

    }

    public void addRRInterval(HeartRateVariabilityAnalysis.RRInterval rr) {
        rrIntervals.addFirst(rr);
        setItems(rrIntervals);
    }
}
