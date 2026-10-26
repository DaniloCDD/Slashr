package com.slashr.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "bot_state")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class BotState {
    public static final String LAST_UPDATE_ID = "last_update_id";

    @Id
    @Column(length = 50)
    private String nome;

    @Column(nullable = false)
    private Long valor;

}
