import java.sql.SQLException;
import java.time.LocalDateTime;
import java.time.ZoneId;

import org.junit.Test;

import static org.junit.Assert.*;

public class BannerControlTest {

    @Test
    public void determineBannerDisplayTest() throws SQLException {
        BannerControl bc = new BannerControl();
        // 過去
        bc.registerBanner("https://locale_kako.png", LocalDateTime.of(2018, 11, 1, 0, 0, 0), LocalDateTime.of(2018, 11, 30, 23, 59, 59));
        // 過去-現在
        bc.registerBanner("https://locale_kako_genzai.png", LocalDateTime.of(2018, 11, 1, 0, 0, 0), LocalDateTime.now());
        // 現在
        bc.registerBanner("https://locale_genzai.png", LocalDateTime.now(), LocalDateTime.now());
        // 現在-未来
        bc.registerBanner("https://locale_genzai_mirai.png", LocalDateTime.now(), LocalDateTime.of(2020, 12, 31, 23, 59, 59));
        // 常識外の未来
        bc.registerBanner("https://locale_mirai.png", LocalDateTime.of(2100, 11, 1, 0, 0, 0), LocalDateTime.of(2100, 11, 30, 23, 59, 59));


        // 過去のバナーは非表示
        assertEquals("", bc.determineBannerDisplay(1, "10.0.0.0", "Asia/Tokyo"));
        // 許可済みIPは表示
        assertEquals("https://locale_kako.png", bc.determineBannerDisplay(1, "10.0.0.1", "Asia/Tokyo"));
        assertEquals("https://locale_kako.png", bc.determineBannerDisplay(1, "10.0.0.2", "Asia/Tokyo"));

        // 現在時間と同じなので一致
        assertEquals("https://locale_kako_genzai.png", bc.determineBannerDisplay(2, "10.0.0.0", "America/Los_Angeles"));
        assertEquals("https://locale_genzai.png", bc.determineBannerDisplay(3, "10.0.0.0", "Europe/Berlin"));
        assertEquals("https://locale_genzai_mirai.png", bc.determineBannerDisplay(4, "10.0.0.0", "Asia/Singapore"));

        // 未来のバナーは非表示
        assertEquals("", bc.determineBannerDisplay(1, "10.0.0.0", "Asia/Tokyo"));
        // 許可済みIPは表示
        assertEquals("https://locale_mirai.png", bc.determineBannerDisplay(5, "10.0.0.1", "Asia/Tokyo"));
        assertEquals("https://locale_mirai.png", bc.determineBannerDisplay(5, "10.0.0.2", "Asia/Tokyo"));


    }

    @Test
    public void isAllowedIpTest() throws SQLException {
        BannerControl bc = new BannerControl();
        assertTrue(bc.isAllowedIp("10.0.0.1"));
        assertTrue(bc.isAllowedIp("10.0.0.2"));
        assertFalse(bc.isAllowedIp("10.0.0.0"));
        assertFalse(bc.isAllowedIp(null));
    }

    @Test
    public void isAllowedTermTest() throws SQLException {
        BannerControl bc = new BannerControl();
        LocalDateTime now = LocalDateTime.now().withNano(0);
        assertTrue(bc.isAllowedTerm(now.toString(), now.toString(), ZoneId.of("Asia/Tokyo")));

    }
}
