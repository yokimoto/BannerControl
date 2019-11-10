import java.sql.*;
import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class BannerControl {

    private static String DB_URL = "jdbc:h2:mem:example;DB_CLOSE_DELAY=-1";


    BannerControl() throws SQLException {
        createBannerTable();
    }

    /**
     * 表示期間を判定し、バナーURLを返す
     * ただし以下の固定IPアドレスでのアクセスからは期間関係なくバナーURLを返す
     * <p>
     * 10.0.0.1 10.0.0.2
     *
     * @param bannerId 表示するバナーID
     * @param ipAdress リクエスト元のIPアドレス
     * @param timezone ブラウザから取得したタイムゾーン文字列（UTC, Asia/Tokyo etc...）
     * @return バナーのURL 表示期間外であれば空文字を返す
     * @throws SQLException DBアクセスエラー
     */
    String determineBannerDisplay(int bannerId, String ipAdress, String timezone) throws SQLException {

        String bannerUrl = "";
        String startTime = "";
        String endTime = "";

        String sql = "select url, startTime, endTime from banner where id = ?";
        ResultSet rs = null;

        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, bannerId);
            rs = stmt.executeQuery();

            if (rs.next()) {

                bannerUrl = rs.getString("url");
                startTime = rs.getString("startTime").substring(0, 19);
                endTime = rs.getString("endTime").substring(0, 19);

                if (!isAllowedIp(ipAdress)) {
                    if (!isAllowedTerm(startTime, endTime, convertTimezoneToZoneId(timezone))) {
                        bannerUrl = "";
                    }
                }
            }
        } finally {
            if (rs != null) {
                rs.close();
            }
        }

        return bannerUrl;

    }

    /**
     * バナーを登録する
     * DBへの登録日時はUTCに変換する
     *
     * @param bannerUrl バナーURL
     * @param startTime バナー表示開始時間
     * @param endTime バナー表示終了時間
     * @throws SQLException DBアクセスエラー
     */
    void registerBanner(String bannerUrl, LocalDateTime startTime, LocalDateTime endTime) throws SQLException {
        // UTCに変換
        LocalDateTime utcStartTime = convertDefaultTimeZoneToUTC(startTime);
        LocalDateTime utcEndTime = convertDefaultTimeZoneToUTC(endTime);

        String sql = "insert into  banner("
                + "url,startTime, endTime ) values (?, ?, ?)";

        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setString(1, bannerUrl);
            stmt.setString(2, utcStartTime.toString());
            stmt.setString(3, utcEndTime.toString());
            stmt.executeUpdate();

        }
    }

    /**
     * バナーを削除する
     * @param bannerId 削除するバナーのID
     * @throws SQLException DBアクセスエラー
     */
    void deleteBanner(int bannerId) throws SQLException {

        String sql = "delete from banner where bannerId = ? ";

        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.setInt(1, bannerId);
            stmt.executeUpdate();

        }
    }

    /**
     * すべてのバナーのリストを取得する
     *
     * @return 登録したバナーのリスト
     * @throws SQLException DBアクセスエラー
     */
    List<Banner> fetchBannerList() throws SQLException {
        List<Banner> bannerList = new ArrayList<>();
        String sql = "select id, url, startTime, endTime from banner ";

        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = con.prepareStatement(sql); ResultSet rs = stmt.executeQuery()) {

            while (rs.next()) {

                Banner banner = new Banner();
                banner.setBannerId(rs.getInt("id"));
                banner.setBannerUrl(rs.getString("url"));
                banner.setStartTime(rs.getString("startTime").substring(0, 19));
                banner.setEndTime(rs.getString("endTime").substring(0, 19));
                bannerList.add(banner);
            }

        }

        return bannerList;

    }

    /**
     * バナーテーブルを作成する
     *
     * @throws SQLException DBアクセスエラー
     */
    void createBannerTable() throws SQLException {
        String sql = "create table if not exists banner("
                + "id integer AUTO_INCREMENT,"
                + "url text NOT NULL,"
                + "startTime datetime NOT NULL,"
                + "endTime datetime NOT NULL,"
                + "PRIMARY KEY (id))";

        try (Connection con = DriverManager.getConnection(DB_URL);
             PreparedStatement stmt = con.prepareStatement(sql)) {

            stmt.executeUpdate();

        }
    }

    /**
     * 許可済みIPアドレスかどうか判定する
     *
     * @param ipAddress 判定するIPアドレス
     * @return 許可済みならtrue そうでない場合はflase
     */
    boolean isAllowedIp(String ipAddress) {
        // 今後増える場合はDB等への移行を検討
        String allowedIp1 = "10.0.0.1";
        String allowedIp2 = "10.0.0.2";

        return allowedIp1.equals(ipAddress) || allowedIp2.equals(ipAddress);

    }

    /**
     * バナーの表示対象期間か判定する
     *
     * @param startTime バナー表示開始時間（UTC）
     * @param endTime バナー表示終了時間（UTC）
     * @param zoneId 表示するブラウザのタイムゾーンのID
     * @return 表示期間 true 非表示期間 false
     */
    boolean isAllowedTerm(String startTime, String endTime, ZoneId zoneId) {
        // ブラウザのタイムゾーンの現在時刻に一旦変換し、それをUTCにして判定する(ナノ秒は切り捨て)
        LocalDateTime currentTime = convertTimeZone(LocalDateTime.now(zoneId), zoneId, ZoneId.of("UTC")).withNano(0);
        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
        return !(LocalDateTime.parse(startTime, fmt).isAfter(currentTime) || LocalDateTime.parse(endTime, fmt).isBefore(currentTime));

    }

    // convert utils

    ZoneId convertTimezoneToZoneId(String timezone) {
        Set<String> zoneIds = ZoneId.getAvailableZoneIds();
        if (zoneIds.contains(timezone)) {
            return ZoneId.of(timezone);
        } else {
            return ZoneId.of("UTC");
        }
    }

    private LocalDateTime convertDefaultTimeZoneToUTC(LocalDateTime ldt) {
        return convertTimeZone(ldt, ZoneId.systemDefault(), ZoneId.of("UTC"));
    }

    private LocalDateTime convertUTCtoDefaultTimeZone(LocalDateTime ldt) {
        return convertTimeZone(ldt, ZoneId.of("UTC"), ZoneId.systemDefault());
    }

    private LocalDateTime convertTimeZone(LocalDateTime ldt, ZoneId fromTimeZone, ZoneId toTimeZone) {
        ZonedDateTime zdt = ZonedDateTime.of(ldt, fromTimeZone);
        return zdt.withZoneSameInstant(toTimeZone).toLocalDateTime();
    }

}
