package com.ciji.serenity.model;

import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

import lombok.Data;
import lombok.ToString;

@Data
@RedisHash
public class CharacterSheetDetails {
    
    @Id
    @ToString.Include
    private String name;

    @Indexed
    private SheetMatrix specialsMatrix;

    @Indexed
    private SheetMatrix skillMatrix;
}
