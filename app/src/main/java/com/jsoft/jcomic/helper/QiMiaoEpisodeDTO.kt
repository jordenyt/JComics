package com.jsoft.jcomic.helper

import java.io.Serializable
import java.util.ArrayList

class QiMiaoEpisodeDTO : Serializable {
    var errorMessage: QiMiaoErrMsg? = null
    var listImg = ArrayList<String>()

    init {
        this.listImg = ArrayList()
    }
}