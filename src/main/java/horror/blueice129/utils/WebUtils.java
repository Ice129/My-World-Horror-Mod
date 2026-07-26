package horror.blueice129.utils;

import java.io.IOException;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;
import java.util.ArrayList;
import java.util.List;

import horror.blueice129.HorrorMod129;
import net.fabricmc.loader.api.FabricLoader;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public final class WebUtils {

    private static final Pattern INVALID_FILE_CHARS = Pattern.compile("[^a-zA-Z0-9._-]+");
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyyMMdd-HHmmss");

    private WebUtils() {
    }

    public static String getPageContent(String url) throws IOException {
        return Jsoup.connect(url).timeout(10000).get().html();
    }

    public static String getPageHtml(String url) throws IOException {
        return Jsoup.connect(url).timeout(10000).get().outerHtml();
    }

    public static Path savePageHtmlToLog(String url) throws IOException {
        String pageHtml = getPageHtml(url);
        Path logsDirectory = FabricLoader.getInstance().getGameDir().resolve("logs").resolve("web-html-dumps");
        Files.createDirectories(logsDirectory);

        String fileName = buildDumpFileName(url);
        Path outputFile = logsDirectory.resolve(fileName);
        Files.writeString(outputFile, pageHtml, StandardCharsets.UTF_8,
            StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        HorrorMod129.LOGGER.info("Saved HTML dump for {} to {}", url, outputFile.toAbsolutePath());
        return outputFile;
    }

    private static String buildDumpFileName(String url) {
        String cleanedUrl;
        try {
            URI uri = URI.create(url);
            String host = uri.getHost() == null || uri.getHost().isBlank() ? "page" : uri.getHost();
            String path = uri.getPath() == null || uri.getPath().isBlank() ? "root" : uri.getPath();
            cleanedUrl = host + path;
        } catch (Exception e) {
            cleanedUrl = url;
        }

        String safeName = INVALID_FILE_CHARS.matcher(cleanedUrl).replaceAll("_");
        safeName = safeName.replaceAll("_+", "_");
        safeName = safeName.replaceAll("^_+|_+$", "");

        if (safeName.isBlank()) {
            safeName = "page";
        }

        if (safeName.length() > 80) {
            safeName = safeName.substring(0, 80);
        }

        return "html-dump-" + LocalDateTime.now().format(TIMESTAMP_FORMAT) + "-" + safeName + ".html";
    }

    public static List<String> getNameMcUsernameHistory(String url) throws IOException {
        Document doc = Jsoup.connect(url).timeout(10000).get();
        List<String> usernames = new ArrayList<>();

        Elements cards = doc.select(".card");
        for (Element card : cards) {
            Element header = card.selectFirst(".card-header strong");
            if (header != null && "Name History".equals(header.text())) {
                Elements rows = card.select("table tbody tr");
                for (Element row : rows) {
                    Element link = row.selectFirst("a[translate=no]");
                    if (link != null) {
                        String username = link.text().trim();
                        if (!username.isEmpty()) {
                            usernames.add(username);
                        }
                    }
                }
                break;
            }
        }

        return usernames;
    }
}
