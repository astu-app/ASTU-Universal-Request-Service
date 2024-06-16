package org.astu.service

import org.apache.poi.xwpf.usermodel.XWPFDocument
import org.apache.poi.xwpf.usermodel.XWPFParagraph
import org.astu.dto.TemplateFieldDTO
import org.koin.core.annotation.Factory
import java.io.File
import java.io.InputStream
import java.io.OutputStream

@Factory
class DocDocumentService {
    /**
     * Шаблон захвата полей
     */
    private val regex = Regex("\\$\\{\\{(.*?)}}")

    /**
     * Прочтение всех полей из потока
     */
    fun readFields(inputStream: InputStream): List<String> {
        val doc = XWPFDocument(inputStream)
        val list = mutableListOf<String>()
        doc.paragraphs.forEach { findInParagraph(it, list) }
        doc.tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    cell.paragraphs.forEach { findInParagraph(it, list) }
                }
            }
        }
        return list.distinct()
    }

    /**
     * Поиск полей в параграфе
     */
    private fun findInParagraph(paragraph: XWPFParagraph, list: MutableList<String>) {
        val text = paragraph.paragraphText
        println(text)
        regex.findAll(paragraph.paragraphText).forEach { match ->
            for (i in 1..<match.groupValues.size) {
                val value = match.groupValues[i]
                list.add(value)
            }
        }
    }

    /**
     * Замена полей в файле
     */
    fun replaceFields(file: File, fields: List<TemplateFieldDTO>, outputStream: OutputStream) {
        val doc = XWPFDocument(file.inputStream())
        doc.paragraphs.forEach { replaceInParagraph(it, fields) }
        doc.tables.forEach { table ->
            table.rows.forEach { row ->
                row.tableCells.forEach { cell ->
                    cell.paragraphs.forEach { replaceInParagraph(it, fields) }
                }
            }
        }
        doc.write(outputStream)
    }

    /**
     * Замена полей в параграфе файла
     */
    private fun replaceInParagraph(paragraph: XWPFParagraph, fields: List<TemplateFieldDTO>) {
        if (regex.findAll(paragraph.text).any()) {
            paragraph.runs.forEach { run ->
                var text = run.text()
                fields.filter { field -> run.text().contains(field.name.coverBrace()) }.forEach { field ->
                    text = text.replace(field.name.coverBrace(), field.value)
                }
                run.setText(text, 0)
            }
        }
    }

    /**
     * Обернуть поле в шаблон захвата полей
     */
    private fun String.coverBrace() = "\${{$this}}"
}