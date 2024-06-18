package com.ciji.serenity.message;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.ArrayList;

import static com.fasterxml.jackson.annotation.JsonInclude.Include;

@JsonInclude(Include.NON_DEFAULT)
public class MessageComponent {

    private String content;

    private int type;

    private int style;

    private String label;

    private String emoji;

    @JsonProperty("custom_id")
    private String customId;

    private String url;

    private boolean disabled;

    private ArrayList<MessageComponent> components;
}
