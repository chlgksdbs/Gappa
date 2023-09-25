package com.sixheadword.gappa.webAlarm.dto.response;

import lombok.*;

import java.time.LocalDateTime;

@AllArgsConstructor
@NoArgsConstructor
@Builder
@Data
public class WebAlarmResponseDto {

    private String toUserName;
    private String toUserProfileImg;
    private LocalDateTime regDate;
    private boolean isRead;
    private LocalDateTime readDate;
    private char alarmCategory;
    private String alarmContent;
}