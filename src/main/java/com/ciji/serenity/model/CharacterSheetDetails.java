package com.ciji.serenity.model;

import java.util.List;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import com.google.api.services.sheets.v4.model.ValueRange;

import lombok.Data;
import lombok.ToString;

@Data
@RedisHash
public class CharacterSheetDetails {
    
    @Id
    @ToString.Include
    private String name;

    @Indexed
    private List<ValueRange> specialsMatrix;

    @Indexed
    private List<ValueRange> skillMatrix;
}
