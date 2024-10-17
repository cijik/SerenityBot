package com.ciji.serenity.model.mapper;


import com.ciji.serenity.model.SheetMatrix;
import com.ciji.serenity.model.SheetRow;
import com.google.api.services.sheets.v4.model.ValueRange;

import java.util.ArrayList;
import java.util.List;
import java.util.NoSuchElementException;

public class SheetMatrixMapper {

    public static SheetMatrix map(List<ValueRange> valueRanges) {
        SheetMatrix sheetMatrix = new SheetMatrix();
        List<String> headers = new ArrayList<>();
        ValueRange valueRangeHeaders = valueRanges.getFirst();
        valueRangeHeaders.getValues().forEach(header -> {
            try {
                headers.add(header.getFirst().toString());
            } catch (NoSuchElementException e) {
                //skip empty columns
            }
        });
        sheetMatrix.setHeaders(headers);
        List<SheetRow> rows = new ArrayList<>();
        ValueRange valueRangeValues = valueRanges.getLast();
        valueRangeValues.getValues().forEach(value -> {
            SheetRow sheetRow = new SheetRow();
            sheetRow.setRow(value.stream().map(Object::toString).toList());

            rows.add(sheetRow);
        });
        sheetMatrix.setRows(rows);
        return sheetMatrix;
    }
}
