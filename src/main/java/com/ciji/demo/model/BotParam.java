package com.ciji.demo.model;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Id;
import javax.persistence.Table;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Entity
@Table(name = "bot_params")
@Data
public class BotParam {

    @Id
    @Column(name = "param_name")
    private String name;

    @Column(name = "param_value")
    private String value;

    @Override
    public String toString() {
        return value;
    }
}
