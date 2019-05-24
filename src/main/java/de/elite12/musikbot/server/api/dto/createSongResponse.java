package de.elite12.musikbot.server.api.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class createSongResponse {
    private boolean success;
    private boolean warn;
    private String message;
}