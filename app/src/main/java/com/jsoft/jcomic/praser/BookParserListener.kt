package com.jsoft.jcomic.praser

import com.jsoft.jcomic.helper.BookDTO

interface BookParserListener {
    fun onBookFetched(book: BookDTO)
}
