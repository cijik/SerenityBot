package com.ciji.serenity.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;

@Entity
@Table(name = "bot_params")
@Data
public class BotParam {

    @Id
    @Column(name = "param_name", length = 50)
    private String name;

    @Column(name = "param_value", length = 50)
    private String value;

    @Override
    public String toString() {
        return value;
    }
}
