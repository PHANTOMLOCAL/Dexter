package ma.dexter.managers

import android.content.Context
import android.content.Intent
import android.widget.Toast
import io.github.rosemoe.sora.widget.CodeEditor
import ma.dexter.core.DexBackedDex
import ma.dexter.core.model.SmaliGotoDef
import ma.dexter.core.model.SmaliModel
import ma.dexter.model.tree.DexClassItem
import ma.dexter.tools.smali.BaksmaliInvoker
import ma.dexter.ui.activity.code.JavaViewerActivity
import ma.dexter.ui.activity.code.SmaliEditorActivity
import ma.dexter.util.FIELD_METHOD_CALL_REGEX
import ma.dexter.util.Releasable
import ma.dexter.util.getClassDefPath
import ma.dexter.util.tokenizeSmali
import org.jf.dexlib2.dexbacked.DexBackedClassDef
import org.jf.smali.smaliParser

object DexProjectManager {
    var dexList: List<DexBackedDex>? = null

    private var smalis = mutableMapOf<DexClassItem, SmaliModel>()

    // their values will be set in this class only, hence the "private set"s
    var currentGotoDef = Releasable<SmaliGotoDef>()
        private set
    var currentJavaCode = Releasable<String>()
        private set

    /**
     * Goes to the class definition that the cursor is at, of the given [CodeEditor].
     * (The text doesn't necessarily have to be selected)
     *
     * Selected text will be stretched for more flexibility, for example
     * consider the following smali line:
     *
     * `sput-object v0, LsomePackage/someClass;->someField:I`
     *
     * If `somePackage` is selected, it will be stretched from both sides
     * to match `LsomePackage/someClass;` and will redirect to that class.
     *
     * If `someField` is selected, it will be stretched from both sides
     * to match `LsomePackage/someClass;->someField:I` and will redirect
     * to that field definition.
     */
    fun gotoDef(context: Context, editor: CodeEditor) {
        val cursor = editor.cursor

        // look for class defs
        tokenizeSmali(editor.text.toString()) { token, line, startColumn ->

            if (token.type == smaliParser.CLASS_DESCRIPTOR && line == cursor.leftLine) {
                val endColumn = startColumn + token.text.length

                if (cursor.leftColumn in startColumn..endColumn &&
                    cursor.rightColumn in startColumn..endColumn) {

                    editor.setSelectionRegion(
                        cursor.leftLine, startColumn,
                        cursor.rightLine, endColumn
                    )

                    gotoClassDef(context, token.text)
                    return
                }
            }
        }

        // look for field/method calls (on a line-by-line basis)
        editor.text.toString().lines().forEachIndexed { lineNumber, line ->

            FIELD_METHOD_CALL_REGEX.matchEntire(line)?.let {
                val range = it.groups[1]!!.range

                if (cursor.leftLine == lineNumber &&
                    cursor.leftColumn in range) {

                    editor.setSelectionRegion(
                        cursor.leftLine,
                        range.first + 0,
                        cursor.rightLine,
                        range.last + 1 // to make it exclusive
                    )

                    val (_, definingClass, descriptor) = it.destructured
                    gotoClassDef(context, definingClass, descriptor)
                    return
                }
            }
        }

    }

    fun gotoClassDef(
        context: Context,
        dexClassDef: String,
        defDescriptorToGo: String? = null
    ) {
        dexList?.forEach { dex ->
            dex.findClassDefByDescriptor(dexClassDef)?.let {
                gotoClassDef(context, SmaliGotoDef(it, defDescriptorToGo))
                return
            }
        }

        Toast.makeText(context, "Couldn't find class def: $dexClassDef", Toast.LENGTH_SHORT).show()
    }

    fun gotoClassDef(
        context: Context,
        dexClassDef: DexBackedClassDef
    ) {
        gotoClassDef(context, SmaliGotoDef(dexClassDef))
    }

    fun gotoClassDef(
        context: Context,
        smaliGotoDef: SmaliGotoDef
    ) {
        currentGotoDef.value = smaliGotoDef

        context.startActivity(
            Intent(context, SmaliEditorActivity::class.java)
        )
    }

    fun gotoJavaViewer(context: Context, javaCode: String) {
        currentJavaCode.value = javaCode

        context.startActivity(
            Intent(context, JavaViewerActivity::class.java)
        )
    }

    // Smali

    fun getSmaliModel(smaliCode: String): SmaliModel {
        return SmaliModel.create(smaliCode)
    }

    fun getSmaliModel(dexClassDef: DexBackedClassDef): SmaliModel {
        return getSmaliModel(
            DexClassItem(
                getClassDefPath(dexClassDef.type),
                dexClassDef
            )
        )
    }

    fun getSmaliModel(dexClassItem: DexClassItem): SmaliModel {
        smalis[dexClassItem]?.let {
            return it
        }

        val smaliFile = SmaliModel.create(
            BaksmaliInvoker.disassemble(dexClassItem.dexClassDef)
        )

        smaliFile.run {
            smalis[dexClassItem] = this
            return this
        }
    }

}