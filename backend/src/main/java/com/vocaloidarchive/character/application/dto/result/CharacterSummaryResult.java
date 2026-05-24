package com.vocaloidarchive.character.application.dto.result;

public record CharacterSummaryResult(
    Long id, String name, String colorHex, String imageUrl, long songCount) {}
