package me.devsaki.hentoid.parsers.images;

import android.util.Pair;

import androidx.annotation.NonNull;

import com.annimon.stream.Stream;

import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import me.devsaki.hentoid.database.domains.Content;
import me.devsaki.hentoid.enums.Site;
import me.devsaki.hentoid.parsers.ParseHelper;
import me.devsaki.hentoid.util.exception.PreparationInterruptedException;

import static me.devsaki.hentoid.util.network.HttpHelper.getOnlineDocument;

/**
 * Created by robb_w on 2021/02
 * Handles parsing of content from toonily.com
 */
public class ToonilyParser extends BaseImageListParser {

    @Override
    protected List<String> parseImages(@NonNull Content content) throws Exception {
        List<String> result = new ArrayList<>();

        List<Pair<String, String>> headers = new ArrayList<>();
        ParseHelper.addSavedCookiesToHeader(content.getDownloadParams(), headers);

        // 1. Scan the gallery page for chapter URLs
        List<String> chapterUrls = new ArrayList<>();
        Document doc = getOnlineDocument(content.getGalleryUrl(), headers, Site.TOONILY.useHentoidAgent(), Site.TOONILY.useWebviewAgent());
        if (doc != null) {
            List<Element> chapters = doc.select("[class^=wp-manga-chapter] a");
            for (Element e : chapters) {
                String link = e.attr("href");
                if (!chapterUrls.contains(link))
                    chapterUrls.add(link); // Make sure we're not adding duplicates
            }
        }
        Collections.reverse(chapterUrls); // Put the chapters in the correct reading order

        progressStart(content.getId(), chapterUrls.size());

        // 2. Open each chapter URL and get the image data until all images are found
        for (String url : chapterUrls) {
            if (processHalted) break;
            doc = getOnlineDocument(url, headers, Site.TOONILY.useHentoidAgent(), Site.TOONILY.useWebviewAgent());
            if (doc != null) {
                List<Element> images = doc.select(".reading-content img");
                result.addAll(Stream.of(images).map(i -> i.attr("data-src").trim()).filterNot(String::isEmpty).toList());
            }
            progressPlus();
        }
        progressComplete();

        // If the process has been halted manually, the result is incomplete and should not be returned as is
        if (processHalted) throw new PreparationInterruptedException();

        return result;
    }
}
