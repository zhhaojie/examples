package com.qimiao.social.calendars;

import lombok.Getter;

@Getter
public final class Apps {

    @Getter
    public static class Google {
        public static final String APPLICATION_NAME = "NBExampleApplication";
        public static final String CLIENT_ID = "768944951916-ik6spf8bdt6f9jk9l96u2f1bos12upgl.apps.googleusercontent.com";
        public static final String CLIENT_SECRET = "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";
        public static final String CALL_BACK_URL = "https://82ef-223-213-179-251.ngrok-free.app/notifications/google";
    }

    @Getter
    public static class Outlook {
        public static final String APPLICATION_NAME = "NBExampleApplication";
        public static final String CLIENT_ID = "a1a42d95-2a30-495e-ab6e-311e9611b801";
        public static final String CLIENT_SECRET = "4bs8Q~CRCzVd_qACFaLOv5tFUWhpaUHDSpAKJahj";
        public static final String CALL_BACK_URL = "https://82ef-223-213-179-251.ngrok-free.app/notifications/outlook";
    }
}
