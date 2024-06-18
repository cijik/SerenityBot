package com.ciji.serenity.model;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.index.Indexed;

@Data
@RedisHash
public class CharacterSheet {

    @Id
    @ToString.Include
    private String id;

    @NotNull
    @Size(min = 3, max = 25)
    @ToString.Include
    @Indexed
    private String name;
}
