package com.safeshare.util;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class BotUserAgentFilter {

    private static final List<String> BOT_PATTERNS = List.of(
            "telegrambot", "whatsapp", "slackbot", "facebookexternalhit",
            "twitterbot", "linkedinbot", "discordbot"
    );

    /**
     * Returns true if the User-Agent header belongs to a known link-preview bot/crawler.
     * These should NOT count as real visits or downloads.
     */
    public boolean isBot(String userAgent) {
        if (userAgent == null || userAgent.isBlank()) return false;
        String lower = userAgent.toLowerCase();
        return BOT_PATTERNS.stream().anyMatch(lower::contains);
    }
}
