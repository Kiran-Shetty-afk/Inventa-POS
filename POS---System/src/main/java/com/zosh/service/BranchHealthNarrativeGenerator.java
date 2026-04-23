package com.zosh.service;

import com.zosh.payload.dto.BranchHealthCopilotResponseDTO;

public interface BranchHealthNarrativeGenerator {
    BranchHealthCopilotResponseDTO generateNarrative(BranchHealthCopilotResponseDTO.BranchHealthSupportingMetricsDTO metrics);
}
