package com.ciji.serenity.model.mapper;

import com.ciji.serenity.model.SheetMatrix;
import com.ciji.serenity.model.SheetRow;
import com.google.api.services.sheets.v4.model.ValueRange;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SheetMatrixMapperTest {

    @Test
    void sheetMatrixMapperSkipsEmptyHeadersAndMapsRows() {
        List<List<Object>> headerValues = List.of(
                List.of("Name"),
                List.of(),
                List.of("Value")
        );
        List<List<Object>> rowValues = List.of(
                List.of("Alice", "10"),
                List.of("Bob", "12")
        );

        ValueRange headers = new ValueRange().setValues(headerValues);
        ValueRange rows = new ValueRange().setValues(rowValues);

        SheetMatrix matrix = SheetMatrixMapper.map(List.of(headers, rows));

        assertThat(matrix.getHeaders()).containsExactly("Name", "Value");
        assertThat(matrix.getRows()).hasSize(2);
        SheetRow firstRow = new SheetRow();
        firstRow.setRow(List.of("Alice", "10"));
        SheetRow secondRow = new SheetRow();
        secondRow.setRow(List.of("Bob", "12"));
        assertThat(matrix.getRows().get(0)).isEqualTo(firstRow);
        assertThat(matrix.getRows().get(1)).isEqualTo(secondRow);
    }

    @Test
    void mapInventoryMapsHeadersAndRowsExcludingLastRow() {
        List<List<Object>> valueRanges = List.of(
                List.of("Item", "Weight", "Amount", "Value", "Total weight", "Total value"),
                List.of("Stimpak", "0.5", "3", "25", "1.5", "75"),
                List.of("RadAway", "0.25", "2", "20", "0.5", "40"),
                List.of("Total", "", "", "", "2.0", "115")
        );

        SheetMatrix matrix = SheetMatrixMapper.mapInventory(valueRanges);

        assertThat(matrix.getHeaders()).containsExactly("Item", "Weight", "Amount", "Value", "Total weight", "Total value");
        assertThat(matrix.getRows()).hasSize(2);
        assertThat(matrix.getRows().get(0).getRow()).containsExactly("Stimpak", "0.5", "3", "25", "1.5", "75");
        assertThat(matrix.getRows().get(1).getRow()).containsExactly("RadAway", "0.25", "2", "20", "0.5", "40");
    }

    @Test
    void mapInventoryHandlesNullOrEmptyInput() {
        SheetMatrix nullMatrix = SheetMatrixMapper.mapInventory(null);
        assertThat(nullMatrix.getHeaders()).isEmpty();
        assertThat(nullMatrix.getRows()).isEmpty();

        SheetMatrix emptyMatrix = SheetMatrixMapper.mapInventory(List.of());
        assertThat(emptyMatrix.getHeaders()).isEmpty();
        assertThat(emptyMatrix.getRows()).isEmpty();
    }

    @Test
    void mapInventoryHandlesSingleRowHeaderOnly() {
        List<List<Object>> singleRow = List.of(
                List.of("Item", "Weight", "Amount")
        );

        SheetMatrix matrix = SheetMatrixMapper.mapInventory(singleRow);

        assertThat(matrix.getHeaders()).containsExactly("Item", "Weight", "Amount");
        assertThat(matrix.getRows()).isEmpty();
    }

    @Test
    void mapInventoryHandlesHeaderAndTotalOnly() {
        List<List<Object>> headerAndTotal = List.of(
                List.of("Item", "Weight", "Amount"),
                List.of("Total", "0", "0")
        );

        SheetMatrix matrix = SheetMatrixMapper.mapInventory(headerAndTotal);

        assertThat(matrix.getHeaders()).containsExactly("Item", "Weight", "Amount");
        assertThat(matrix.getRows()).isEmpty();
    }
}