package com.ciji.serenity.model;

import lombok.Data;

import java.util.List;

@Data
public class SheetMatrix {

    private List<String> headers;

    private List<SheetRow> rows;
}
