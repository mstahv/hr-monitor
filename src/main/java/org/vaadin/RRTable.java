package org.vaadin;

import com.vaadin.flow.dom.Style;
import org.vaadin.firitin.fields.internalhtmltable.Table;
import org.vaadin.firitin.fields.internalhtmltable.TableRow;
import org.vaadin.firitin.util.VStyleUtil;

public class RRTable extends Table {
    final static int MAX_ROWS = 200;

    public RRTable() {
        VStyleUtil.inject("""
                td,th {
                    padding: 0 1em 0 0;
                    text-align:left;
                    white-space:nowrap;
                }
                """);

        TableRow headerRow = getHead().addRow();
        headerRow.addHeaderCell().setText("idx");
        headerRow.addHeaderCell().setText("ms");
        headerRow.addHeaderCell().setText("diff");
        headerRow.addHeaderCell().setText("SD (last 5)");
        headerRow.addHeaderCell().setText("HRV (10)");
    }

    public void addRRInterval(HeartRateVariabilityAnalysis.RRInterval rr) {
        TableRow r = getBody().insertRow(0);
        r.addDataCell().setText(rr.index()+"");
        r.addDataCell().setText(rr.duration()+"");
        r.addDataCell().setText(rr.diff()+"");
        r.addDataCell().setText(String.format("%.2f",rr.std()));
        r.addDataCell().setText(String.format("%.2f",rr.rms10()));

        if(rr.relToAvg() != null) {
            // highlight rows if something weird is happening, "extra beats" etc
            double relativeChangePercentage = Math.abs(rr.relToAvg() - 100);
            if(relativeChangePercentage > 20) {
                // most likely "missed a beat" or extra beat
                r.getStyle().setBackground("red");
            } else if(relativeChangePercentage > 4) {
                // Some other large variance in r-r, most likely just signs of good fit
                r.getStyle().setBackground("yellow");
            }
        }

        if(getBody().getRows().size() > MAX_ROWS) {
            getBody().removeRow(MAX_ROWS);
        }
    }
}
