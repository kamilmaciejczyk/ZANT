package com.zant.backend.dto.ewyp;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DocumentsToDeliverLaterDTO {
    private String toDate;
    private List<String> documents;
}
