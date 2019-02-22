package com.jsoft.jcomic.praser;

import com.jsoft.jcomic.helper.BookDTO;

public interface BookParserListener {
    void onBookFetched(BookDTO book);
}
