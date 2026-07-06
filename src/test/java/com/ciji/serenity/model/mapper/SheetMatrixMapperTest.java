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
}