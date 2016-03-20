package com.erakk.lnreader.helper.db;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.erakk.lnreader.Constants;
import com.erakk.lnreader.helper.DBHelper;
import com.erakk.lnreader.model.BookModel;
import com.erakk.lnreader.model.NovelCollectionModel;
import com.erakk.lnreader.model.PageModel;

import java.util.ArrayList;
import java.util.Date;

public class BookModelHelper {
    // New column should be appended as the last column
    // COLUMN_PAGE is not unique because being used for reference to the novel page.
    public static final String DATABASE_CREATE_NOVEL_BOOKS = "create table if not exists "
            + DBHelper.TABLE_NOVEL_BOOK + "(" + DBHelper.COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " // 0
            + DBHelper.COLUMN_PAGE + " text not null, " // 1
            + DBHelper.COLUMN_TITLE + " text not null, " // 2
            + DBHelper.COLUMN_LAST_UPDATE + " integer, " // 3
            + DBHelper.COLUMN_LAST_CHECK + " integer, " // 4
            + DBHelper.COLUMN_ORDER + " integer);"; // 5
    private static final String TAG = BookModelHelper.class.toString();

    public static BookModel cursorToBookModel(Cursor cursor) {
        BookModel book = new BookModel();
        book.setId(cursor.getInt(0));
        book.setPage(cursor.getString(1));
        book.setTitle(cursor.getString(2));
        book.setLastUpdate(new Date(cursor.getInt(3) * 1000));
        book.setLastCheck(new Date(cursor.getInt(4) * 1000));
        book.setOrder(cursor.getInt(5));
        return book;
    }

	/*
     * Query Stuff
	 */

    public static BookModel getBookModel(DBHelper helper, SQLiteDatabase db, int id) {
        BookModel book = null;
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
                + " where " + DBHelper.COLUMN_ID + " = ? ", new String[]{"" + id});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                book = cursorToBookModel(cursor);
                // Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return book;
    }

    public static BookModel getBookModel(DBHelper helper, SQLiteDatabase db, String page, String title) {
        BookModel book = null;
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
                + " where " + DBHelper.COLUMN_PAGE + " = ? and " + DBHelper.COLUMN_TITLE + " = ?", new String[]{page, title});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                book = cursorToBookModel(cursor);
                Log.d(TAG, "Found: " + book.getPage() + Constants.NOVEL_BOOK_DIVIDER + book.getTitle());
                break;
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return book;
    }

    public static ArrayList<BookModel> getBookCollectionOnly(DBHelper helper, SQLiteDatabase db, String page, NovelCollectionModel novelDetails) {
        // get the books
        ArrayList<BookModel> bookCollection = new ArrayList<BookModel>();
        Cursor cursor = helper.rawQuery(db, "select * from " + DBHelper.TABLE_NOVEL_BOOK
                + " where " + DBHelper.COLUMN_PAGE + " = ? "
                + " order by " + DBHelper.COLUMN_ORDER + ", " + DBHelper.COLUMN_TITLE, new String[]{page});
        try {
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BookModel book = cursorToBookModel(cursor);
                book.setParent(novelDetails);
                bookCollection.add(book);
                //Log.d(TAG, "Found: " + book.toString() + " Order: " + book.getOrder());
                cursor.moveToNext();
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return bookCollection;
    }

	/*
	 * Insert Stuff
	 */

    public static BookModel insertBookModel(DBHelper helper, SQLiteDatabase db, BookModel book) {
        ContentValues cv2 = new ContentValues();
        cv2.put(DBHelper.COLUMN_PAGE, book.getPage());
        cv2.put(DBHelper.COLUMN_TITLE, book.getTitle());
        cv2.put(DBHelper.COLUMN_ORDER, book.getOrder());
        cv2.put(DBHelper.COLUMN_LAST_CHECK, "" + (int) (new Date().getTime() / 1000));

        BookModel tempBook = BookModelHelper.getBookModel(helper, db, book.getId());
        if (tempBook == null)
            tempBook = BookModelHelper.getBookModel(helper, db, book.getPage(), book.getTitle());
        if (tempBook == null) {
            // Log.d(TAG, "Inserting Novel Book: " + novelDetails.getPage() + Constants.NOVEL_BOOK_DIVIDER +
            // book.getTitle());
            if (book.getLastUpdate() == null)
                cv2.put(DBHelper.COLUMN_LAST_UPDATE, 0);
            else
                cv2.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (book.getLastUpdate().getTime() / 1000));
            helper.insertOrThrow(db, DBHelper.TABLE_NOVEL_BOOK, null, cv2);
        } else {
            // Log.d(TAG, "Updating Novel Book: " + tempBook.getPage() + Constants.NOVEL_BOOK_DIVIDER +
            // tempBook.getTitle() + " id: " + tempBook.getId());
            cv2.put(DBHelper.COLUMN_LAST_UPDATE, "" + (int) (tempBook.getLastUpdate().getTime() / 1000));
            helper.update(db, DBHelper.TABLE_NOVEL_BOOK, cv2, DBHelper.COLUMN_ID + " = ?", new String[]{"" + tempBook.getId()});
        }

        book = getBookModel(helper, db, book.getPage(), book.getTitle());

        return book;
    }

	/*
	 * Delete Stuff
	 */

    /***
     * Delete Book Model with it's chapter and it's content.
     *
     * @param db
     * @param book
     * @return
     */
    public static int deleteBookModel(DBHelper helper, SQLiteDatabase db, BookModel book) {
        int chaptersCount = 0;
        int contentCount = 0;
        ArrayList<PageModel> chapters = book.getChapterCollection();
        if (chapters != null && chapters.size() > 0) {
            for (PageModel chapter : chapters) {
                contentCount += NovelContentModelHelper.deleteNovelContent(helper, db, chapter);
            }
            Log.w(TAG, "Deleted NovelContent: " + contentCount);

            for (PageModel chapter : chapters) {
                chaptersCount += helper.delete(db, DBHelper.TABLE_PAGE, DBHelper.COLUMN_ID + " = ? ", new String[]{"" + chapter.getId()});
                ImageModelHelper.deleteImageByParent(helper, db, chapter.getPage());
            }
            Log.w(TAG, "Deleted PageModel: " + chaptersCount);
        }

        int bookCount = helper.delete(db, DBHelper.TABLE_NOVEL_BOOK, DBHelper.COLUMN_ID + " = ? ", new String[]{"" + book.getId()});
        Log.w(TAG, "Deleted BookModel: " + bookCount);
        return bookCount;
    }
}
