package com.fromzerotohero.mission.intake;

public record RequestAnalysisInput(
        String title,
        String details,
        RequestCategory requestedCategory,
        RequestUrgency requestedUrgency) {}
