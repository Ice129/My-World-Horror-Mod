package horror.blueice129.utils;

import java.io.IOException;

import org.jsoup.Jsoup;

public final class WebUtils {

    private WebUtils() {
    }

    public static String getPageContent(String url) throws IOException {
        return Jsoup.connect(url).timeout(10000).get().html();
    }
}
