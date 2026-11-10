package com.casaverde.backend.fachadaServices.dto;

import java.time.LocalDateTime;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BackupDTO {
    private String nombre;
    private String path;
    private Long bytes;
    private LocalDateTime creadoEn;
}
