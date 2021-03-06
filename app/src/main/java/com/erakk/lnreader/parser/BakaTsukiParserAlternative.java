/**
 * Parse baka-tsuki wiki page - Alternative Language
 */
package com.erakk.lnreader.parser;

import android.util.Log;

import com.erakk.lnreader.AlternativeLanguageInfo;
import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.Util;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.ImageModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.NovelContentModel;
import com.erakk.lnreader.model.PageModel;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.Map.Entry;

/**
 * @author freedomofkeima
 *         Modified from: BakaTsukiParser by Nandaka
 */
public class BakaTsukiParserAlternative {

    private static final String TAG = BakaTsukiParserAlternative.class.toString();

    /**
     * Crawl all sub-categories
     *
     * @param doc
     * @return
     */
    public static ArrayList<String> CrawlAlternativeCategory(Document doc) {
        ArrayList<String> result = new ArrayList<String>();
        if (doc == null)
            throw new NullPointerException("Document cannot be null.");

        Element stage = doc.select("#mw-subcategories").first();

        if (stage != null) {
            Elements list = stage.select("li");

            for (Element element : list) {
                Element link = element.select("a").first();
                String subcategory_link = CommonParser.normalizeInternalUrl(link.attr("href"));
                // Check pattern
                boolean isPatternFound = false;

                for (String pattern : Constants.categoryPattern) {
                    if (subcategory_link.indexOf(pattern) != -1) {
                        isPatternFound = true;
                        break;
                    }
                }

                if (isPatternFound)
                    result.add(subcategory_link);
            }
        }

        return result;
    }

    /**
     * Parse Alternative Language list from http://www.baka-tsuki.org/project/index.php?title=[Alternative Category]
     *
     * @param doc
     * @return
     */
    public static ArrayList<PageModel> ParseAlternativeList(Document doc, String language) {
        ArrayList<PageModel> result = new ArrayList<PageModel>();
        String category = "";
        if (language != null)
            category = AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getCategoryInfo();
        if (doc == null)
            throw new NullPointerException("Document cannot be null.");

        Element stage = doc.select("#mw-pages").first();

        // int order = 0;
        if (stage != null) {
            Elements list = stage.select("li");

            for (Element element : list) {
                Element link = element.select("a").first();
                PageModel page = new PageModel();
                page.setParent(category);
                String tempPage = CommonParser.normalizeInternalUrl(link.attr("href"));
                page.setPage(tempPage);
                page.setLanguage(language);
                page.setType(PageModel.TYPE_NOVEL);
                page.setTitle(link.text());
                page.setStatus(language);
                // page.setOrder(order);

                result.add(page);
                // ++order;
            }
        }
        return result;
    }

    /**
     * Parse novel Title, Synopsis, Cover, and Chapter list.
     *
     * @param doc
     * @param page
     * @return
     */
    public static NovelCollectionModel ParseNovelDetails(Document doc, PageModel page) {
        NovelCollectionModel novel = new NovelCollectionModel();
        if (doc == null)
            throw new NullPointerException("Document cannot be null.");
        novel.setPage(page.getPage());
        novel.setPageModel(page);

        String redirected = CommonParser.redirectedFrom(doc, page);
        novel.setRedirectTo(redirected);

        parseNovelSynopsis(doc, novel, page.getLanguage()); // language-dependent
        CommonParser.parseNovelCover(doc, novel);
        parseNovelChapters(doc, novel, page.getLanguage()); // language-dependent

        parseNovelStatus(doc, page);

        return novel;
    }

    private static PageModel parseNovelStatus(Document doc, PageModel page) {
        boolean isTeaser = page.isTeaser();
        boolean isStalled = page.isStalled();
        boolean isAbandoned = page.isAbandoned();
        boolean isPending = page.isPending();

        // Template:STALLED
        Elements links = doc.select("a[title=Template:STALLED]");
        if (links != null && links.size() > 0) {
            isStalled = true;
            Log.i(TAG, "Novel is stalled: " + page.getPage());
        } else
            isStalled = false;

        // Template:Abandoned
        links = doc.select("a[title=Template:Abandoned]");
        if (links != null && links.size() > 0) {
            isAbandoned = true;
            Log.i(TAG, "Novel is abandoned: " + page.getPage());
        } else
            isAbandoned = false;

        // Template:Warning:ATP
        links = doc.select("a[title=Template:Warning:ATP]");
        if (links != null && links.size() > 0) {
            isPending = true;
            Log.i(TAG, "Novel is pending authorization: " + page.getPage());
        } else
            isPending = false;

        // Teaser => parent = Category:Teasers
        if (page.getParent().equalsIgnoreCase(Constants.ROOT_TEASER)) {
            isTeaser = true;
            Log.i(TAG, "Novel is Teaser Project: " + page.getPage());
        } else
            isTeaser = false;

        // update the status
        ArrayList<String> statuses = new ArrayList<String>();
        if (isTeaser)
            statuses.add(Constants.STATUS_TEASER);
        if (isStalled)
            statuses.add(Constants.STATUS_STALLED);
        if (isAbandoned)
            statuses.add(Constants.STATUS_ABANDONED);
        if (isPending)
            statuses.add(Constants.STATUS_PENDING);

        page.setStatus(Util.join(statuses, "|"));

        return page;
    }

    /**
     * find span with id containing "_by" or 'Full_Text'
     * or contains with Page Name or "Side_Stor*" or "Short_Stor*"
     * or Official_Parody_Stories
     * or contains "_Series" (Maru-MA)
     * or if redirected, use the redirect page name.
     *
     * @param s
     * @param novel
     * @param language
     * @return
     */
    private static boolean validateH2(Element s, NovelCollectionModel novel, String language) {
        ArrayList<String> parserRule = AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getParserInfo();
        parserRule.add(novel.getPage());
        parserRule.add(novel.getRedirectTo());

        for (String rule : parserRule) {
            if (!Util.isStringNullOrEmpty(rule) && s.id().contains(rule)) {
                return true;
            }
        }

        return false;
    }

    private static void parseNovelChapters(Document doc, NovelCollectionModel novel, String language) {
        // Log.d(TAG, "Start parsing book collections for " + novel.getPage());
        // parse the collection
        ArrayList<BookModel> books = new ArrayList<BookModel>();
        boolean oneBookOnly = false;

        try {
            Elements h2s = doc.select("h1,h2");
            for (Element h2 : h2s) {
                // Log.d(TAG, "checking h2: " +h2.text() + "\n" + h2.id());
                Elements spans = h2.select("span");
                if (spans.size() > 0) {
                    boolean containsBy = false;
                    for (Element s : spans) {
                        Log.d(TAG, "Checking: " + s.id());
                        if (validateH2(s, novel, language)) {
                            containsBy = true;
                            Log.d(TAG, "Got valid id: " + s.id());
                            break;
                        }
                        Log.d(TAG, "Not valid id: " + s.id());
                    }
                    if (!containsBy) {
                        continue;
                    }

                    // Log.d(TAG, "Found h2: " +h2.text());
                    ArrayList<BookModel> tempBooks = parseBooksMethod1(novel, h2, language);
                    if (tempBooks != null && tempBooks.size() > 0) {
                        books.addAll(tempBooks);
                    }
                    if (books.size() == 0 || (oneBookOnly && tempBooks.size() == 0)) {
                        Log.d(TAG, "No books found, use method 2: Only have 1 book, chapter in <p> tag.");
                        tempBooks = parseBooksMethod2(novel, h2, language);
                        if (tempBooks != null && tempBooks.size() > 0) {
                            oneBookOnly = true;
                            books.addAll(tempBooks);
                        }
                    }
                    if (books.size() == 0 || (oneBookOnly && tempBooks.size() == 0)) {
                        Log.d(TAG, "No books found, use method 3: Only have 1 book.");
                        tempBooks = parseBooksMethod3(novel, h2, language);
                        if (tempBooks != null && tempBooks.size() > 0) {
                            oneBookOnly = true;
                            books.addAll(tempBooks);
                        }
                    }
                }
            }
        } catch (Exception e) {
            Log.e(TAG, "Unknown Exception for " + novel.getPage() + ": " + e.getMessage(), e);
        }
        // Log.d(TAG, "Complete parsing book collections: " + books.size());

        novel.setBookCollections(CommonParser.validateNovelBooks(books));
    }

    /**
     * Look for <h3>after
     * <h2>containing the volume list. Treat each li in dl/ul/div as the chapters.
     *
     * @param novel
     * @param h2
     * @return
     */
    private static ArrayList<BookModel> parseBooksMethod1(NovelCollectionModel novel, Element h2, String language) {
        // Log.d(TAG, "method 1");
        ArrayList<BookModel> books = new ArrayList<BookModel>();
        Element bookElement = h2;
        boolean walkBook = true;
        int bookOrder = 0;
        do {
            bookElement = bookElement.nextElementSibling();
            if (bookElement == null || bookElement.tagName() == "h2")
                walkBook = false;
            else if (bookElement.tagName() != "h3") {
                Elements h3s = bookElement.select("h3");
                if (h3s != null && h3s.size() > 0) {
                    for (Element h3 : h3s) {
                        bookOrder = CommonParser.processH3(novel, books, h3, bookOrder, language);
                    }
                }
            } else if (bookElement.tagName() == "h3") {
                bookOrder = CommonParser.processH3(novel, books, bookElement, bookOrder, language);
            }
        } while (walkBook);
        return books;
    }

    /**
     * parse book method 2:
     * Look for &lt;p&gt; after &lt;h2&gt; containing the chapter list, usually only have 1 book.
     * See 7_Nights
     *
     * @param novel
     * @param h2
     * @return
     */
    private static ArrayList<BookModel> parseBooksMethod2(NovelCollectionModel novel, Element h2, String language) {
        ArrayList<BookModel> books = new ArrayList<BookModel>();
        Element bookElement = h2;
        boolean walkBook = true;
        int bookOrder = 0;
        do {
            bookElement = bookElement.nextElementSibling();
            if (bookElement == null || bookElement.tagName() == "h2")
                walkBook = false;
            else if (bookElement.tagName() == "p") {
                // Log.d(TAG, "Found: " + bookElement.text());
                BookModel book = new BookModel();
                book.setTitle(CommonParser.sanitize(bookElement.text(), true));
                book.setOrder(bookOrder);
                ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
                String parent = novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle();

                // parse the chapters.
                boolean walkChapter = true;
                int chapterOrder = 0;
                Element chapterElement = bookElement;
                do {
                    chapterElement = chapterElement.nextElementSibling();
                    if (chapterElement == null)
                        walkChapter = false;
                    else if (chapterElement.tagName() == "p")
                        walkChapter = false;
                    else if (chapterElement.tagName() == "dl" || chapterElement.tagName() == "ul" || chapterElement.tagName() == "div") {
                        Elements chapters = chapterElement.select("li");
                        for (Element chapter : chapters) {
                            ArrayList<PageModel> pageModels = CommonParser.processLI(chapter, parent, chapterOrder, language);
                            for (PageModel p : pageModels) {
                                if (p != null) {
                                    chapterCollection.add(p);
                                    ++chapterOrder;
                                }
                            }
                        }
                    }
                    // no subchapter
                    if (chapterCollection.size() == 0) {
                        Elements links = bookElement.select("a");
                        if (links.size() > 0) {
                            Element link = links.first();
                            PageModel p = CommonParser.processA(link.text(), parent, chapterOrder, link, chapterCollection.get(0).getLanguage());
                            // Log.d(TAG, "chapter: " + p.getTitle() + " = " + p.getPage());
                            if (p != null) {
                                chapterCollection.add(p);
                                ++chapterOrder;
                            }
                        }
                    }
                    book.setChapterCollection(chapterCollection);
                } while (walkChapter);
                books.add(book);
                ++bookOrder;
            }
        } while (walkBook);
        return books;
    }

    /**
     * Only have 1 book, chapter list is nested in ul/dl, e.g:Fate/Apocrypha, Gekkou
     * Parse the li as the chapters.
     *
     * @param novel
     * @param h2
     * @return
     */
    private static ArrayList<BookModel> parseBooksMethod3(NovelCollectionModel novel, Element h2, String language) {
        ArrayList<BookModel> books = new ArrayList<BookModel>();
        Element bookElement = h2;
        boolean walkBook = true;
        int bookOrder = 0;
        do {
            bookElement = bookElement.nextElementSibling();
            if (bookElement == null || bookElement.tagName() == "h2")
                walkBook = false;
            else if (bookElement.tagName() == "ul" || bookElement.tagName() == "dl") {
                // Log.d(TAG, "Found: " +bookElement.text());
                BookModel book = new BookModel();
                book.setTitle(CommonParser.sanitize(h2.text(), true));
                book.setOrder(bookOrder);
                ArrayList<PageModel> chapterCollection = new ArrayList<PageModel>();
                String parent = novel.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle();

                // parse the chapters.
                int chapterOrder = 0;
                Elements chapters = bookElement.select("li");
                for (Element chapter : chapters) {
                    ArrayList<PageModel> pageModels = CommonParser.processLI(chapter, parent, chapterOrder, language);
                    for (PageModel p : pageModels) {
                        if (p != null) {
                            chapterCollection.add(p);
                            ++chapterOrder;
                        }
                    }
                }
                book.setChapterCollection(chapterCollection);
                books.add(book);
                ++bookOrder;
            }
        } while (walkBook);
        return books;
    }

    private static String parseNovelSynopsis(Document doc, NovelCollectionModel novel, String language) {
        // Log.d(TAG, "Start parsing synopsis");
        // parse the synopsis
        String synopsis = "";
        String source = "";
        if (language != null)
            source = AlternativeLanguageInfo.getAlternativeLanguageInfo().get(language).getMarkerSynopsis();
        // from Story_Synopsis id
        Elements stage = doc.select(source);// .first().parent().nextElementSibling();
        // from main text
        if (stage == null || stage.size() <= 0) {
            source = "#mw-content-text,p";
            stage = doc.select(source);
            Log.i(TAG, "Synopsis from: " + source);
        }

        if (stage.size() > 0) {
            Element synopsisE = stage.first().children().first();
            Iterator<Entry<String, AlternativeLanguageInfo>> it = AlternativeLanguageInfo.getAlternativeLanguageInfo().entrySet().iterator();
            while (it.hasNext()) {
                AlternativeLanguageInfo info = it.next().getValue();
                if (source.equals(info.getMarkerSynopsis()))
                    synopsisE = stage.first().parent().nextElementSibling();
                it.remove();
            }
            boolean processOne = false;
            if (synopsisE == null || synopsisE.select("p").size() == 0) {
                // cannot found any synopsis, take the first available p
                synopsisE = stage.first();
                processOne = true;
            }

            int i = 0;
            do {
                if (synopsisE == null)
                    break;
                if (synopsisE.tagName() != "p") {
                    synopsisE = synopsisE.nextElementSibling();
                    // Log.d(TAG, synopsisE.html());
                    continue;
                }
                i++;
                synopsis += synopsisE.text() + "\n";
                synopsisE = synopsisE.nextElementSibling();
                if (synopsisE != null && synopsisE.tagName() != "p" && i > 0)
                    break;

                if (i > 10)
                    break; // limit only first 10 paragraph.
                if (processOne)
                    break;
            } while (true);
        }

        novel.setSynopsis(synopsis);
        // Log.d(TAG, "Completed parsing synopsis.");
        return synopsis;
    }

    public static NovelContentModel ParseNovelContent(Document doc, PageModel page) throws Exception {
        NovelContentModel content = new NovelContentModel();
        page.setDownloaded(true);
        content.setPage(page.getPage());
        content.setPageModel(page);

        Element textElement = doc.select("text").first();
        if (textElement == null)
            throw new Exception("Empty content!");
        String text = textElement.text();

        // get valid image list
        Document imgDoc = Jsoup.parse(text);
        ArrayList<ImageModel> images = CommonParser.processImagesFromContent(imgDoc);
        content.setImages(images);
        content.setContent(CommonParser.replaceImagePath(text));

        return content;
    }
}
