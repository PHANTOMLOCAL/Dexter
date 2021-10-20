package ma.dexter.ui.model

import ma.dexter.R
import ma.dexter.model.GotoDef
import ma.dexter.model.JavaGotoDef
import ma.dexter.model.SmaliGotoDef
import ma.dexter.util.getClassDefPath

sealed class DexPageItem(val typeDef: String?) {
    abstract fun getTitle(): String
    abstract fun getIconResId(): Int
    abstract fun getGotoDef(): GotoDef?
}

// guaranteed to be at position 0 (for now)
class MainItem : DexPageItem(null) {
    override fun getTitle() = "TREE"
    override fun getIconResId() = R.drawable.ic_baseline_home_24
    override fun getGotoDef(): GotoDef? = null
}

class SmaliItem(
    val smaliGotoDef: SmaliGotoDef
) : DexPageItem(smaliGotoDef.classDef.type) {

    override fun getTitle() =
        getClassDefPath(smaliGotoDef.classDef.type).substringAfterLast("/") + ".smali"

    override fun getIconResId() = R.drawable.ic_letter_s_24

    override fun getGotoDef() = smaliGotoDef
}

class JavaItem(
    val javaGotoDef: JavaGotoDef
) : DexPageItem(javaGotoDef.className) {

    override fun getTitle() =
        javaGotoDef.className.substringAfterLast("/") + ".java"

    override fun getIconResId() = R.drawable.ic_java

    override fun getGotoDef() = javaGotoDef
}