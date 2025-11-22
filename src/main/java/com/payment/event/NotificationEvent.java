package com.payment.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.util.Map;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NotificationEvent {

    private String userName;
    private String username;
    private String subject;
    private String source;
    private String message;
    private Map<String, String> metadata;
}
