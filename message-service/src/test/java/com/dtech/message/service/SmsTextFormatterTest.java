package com.dtech.message.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SmsTextFormatterTest {
    @Test
    void convertsClaimTemplateBreaksToSmsLines() {
        assertEquals("Claim HC/DTECH/NS/2026/0082 submitted.\n\nThank you!",
                SmsTextFormatter.toPlainText(
                        "Claim HC/DTECH/NS/2026/0082 submitted.<br><br> Thank you!"));
    }

    @Test
    void handlesBreakVariantsAndHtmlEntitiesWithoutChangingPlainText() {
        assertEquals("One\nTwo\nThree & four",
                SmsTextFormatter.toPlainText("<p>One<BR/>Two<br class=\"x\">Three &amp; four</p>"));
        assertEquals("Your OTP is 123456", SmsTextFormatter.toPlainText("Your OTP is 123456"));
    }
}
