package com.vocaloidarchive.song.interfaces.dto.request;

import com.vocaloidarchive.song.domain.Mood;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SongCreateRequest(
    @NotBlank
    @Size(max = 200)
    String title,

    @Pattern(
        regexp = "^(https?://)?(www\\.)?(youtube\\.com|youtu\\.be).*",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "youtube.com 또는 youtu.be URL이어야 합니다")
    @Size(max = 500)
    String youtubeUrl,

    @Pattern(
        regexp = "^(https?://)?(www\\.)?(nicovideo\\.jp|nico\\.ms).*",
        flags = Pattern.Flag.CASE_INSENSITIVE,
        message = "nicovideo.jp 또는 nico.ms URL이어야 합니다")
    @Size(max = 500)
    String niconicoUrl,

    @Min(value = 40, message = "bpm은 40 이상이어야 합니다")
    @Max(value = 300, message = "bpm은 300 이하여야 합니다")
    Integer bpm,

    @NotNull(message = "mood는 필수입니다")
    Mood mood,

    @NotEmpty(message = "캐릭터를 1개 이상 선택해야 합니다")
    @Size(max = 10, message = "캐릭터는 최대 10개까지 가능합니다")
    List<Long> characterIds,

    @Size(max = 10, message = "태그는 최대 10개까지 가능합니다")
    List<@NotBlank @Size(min = 1, max = 30) String> tagNames
) {}
