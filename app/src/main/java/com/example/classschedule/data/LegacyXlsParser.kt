package com.example.classschedule.data

import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import kotlin.math.min

/** Reads the BIFF workbook stream used by Excel 97-2003 files without a runtime service. */
internal object LegacyXlsParser {
    private val compoundSignature = byteArrayOf(
        0xD0.toByte(), 0xCF.toByte(), 0x11, 0xE0.toByte(), 0xA1.toByte(), 0xB1.toByte(), 0x1A, 0xE1.toByte()
    )

    fun readMatrix(input: InputStream): List<List<String>> {
        val bytes = input.readBytes()
        if (bytes.size < 512 || !bytes.copyOfRange(0, compoundSignature.size).contentEquals(compoundSignature)) {
            error("不是有效的旧版 XLS 文件")
        }
        val workbook = CompoundDocument(bytes).readStream("Workbook")
            ?: CompoundDocument(bytes).readStream("Book")
            ?: error("XLS 中找不到 Workbook 工作簿流")
        return BiffWorkbook.readFirstSheet(workbook)
    }
}

private class CompoundDocument(private val bytes: ByteArray) {
    private companion object {
        const val HEADER_SIZE = 512
        const val FREE = 0xFFFFFFFFL
        const val END = 0xFFFFFFFEL
        const val FAT_SECTOR = 0xFFFFFFFDL
        const val DIFAT_SECTOR = 0xFFFFFFFCL
        const val ROOT_TYPE = 5
        const val STREAM_TYPE = 2
    }

    private val sectorSize: Int
    private val miniSectorSize: Int
    private val miniStreamCutoff: Long
    private val fat: LongArray
    private val miniFat: LongArray
    private val miniStream: ByteArray
    private val entries: Map<String, DirectoryEntry>

    init {
        val sectorShift = u16(bytes, 0x1E)
        val miniShift = u16(bytes, 0x20)
        require(sectorShift in 7..20) { "XLS 扇区大小无效" }
        require(miniShift in 3..12) { "XLS mini 扇区大小无效" }
        sectorSize = 1 shl sectorShift.toInt()
        miniSectorSize = 1 shl miniShift.toInt()
        miniStreamCutoff = u32(bytes, 0x38)

        val fatSectorIds = readFatSectorIds()
        fat = LongArray(fatSectorIds.size * (sectorSize / 4))
        fatSectorIds.forEachIndexed { index, sector ->
            val sectorBytes = readSector(sector)
            for (entry in 0 until sectorSize / 4) {
                fat[index * (sectorSize / 4) + entry] = u32(sectorBytes, entry * 4)
            }
        }

        val directoryStart = u32(bytes, 0x30)
        val directoryBytes = readRegularChain(directoryStart, bytes.size.toLong())
        entries = readDirectory(directoryBytes)

        val root = entries.values.firstOrNull { it.type == ROOT_TYPE }
        val miniFatStart = u32(bytes, 0x3C)
        val miniFatCount = u32(bytes, 0x40)
        miniFat = if (miniFatStart != END && miniFatCount > 0) {
            val data = readRegularChain(miniFatStart, miniFatCount * sectorSize)
            LongArray(data.size / 4) { index -> u32(data, index * 4) }
        } else {
            LongArray(0)
        }
        miniStream = if (root != null && root.size > 0) {
            readRegularChain(root.startSector, root.size)
        } else {
            ByteArray(0)
        }
    }

    fun readStream(name: String): ByteArray? {
        val entry = entries.values.firstOrNull { it.type == STREAM_TYPE && it.name.equals(name, true) } ?: return null
        if (entry.size == 0L) return ByteArray(0)
        return if (entry.size < miniStreamCutoff && miniFat.isNotEmpty()) {
            readMiniChain(entry.startSector, entry.size)
        } else {
            readRegularChain(entry.startSector, entry.size)
        }
    }

    private fun readFatSectorIds(): List<Long> {
        val expected = u32(bytes, 0x2C).toInt()
        val result = mutableListOf<Long>()
        for (index in 0 until 109) {
            val sector = u32(bytes, 0x4C + index * 4)
            if (sector != FREE) result += sector
        }
        var difatSector = u32(bytes, 0x44)
        val difatCount = u32(bytes, 0x48).toInt()
        repeat(difatCount) {
            if (difatSector == END || difatSector == FREE) return@repeat
            val sectorBytes = readSector(difatSector)
            for (index in 0 until (sectorSize / 4 - 1)) {
                val sector = u32(sectorBytes, index * 4)
                if (sector != FREE) result += sector
            }
            difatSector = u32(sectorBytes, sectorSize - 4)
        }
        require(result.size >= expected) { "XLS FAT 不完整" }
        return result.take(expected)
    }

    private fun readDirectory(data: ByteArray): Map<String, DirectoryEntry> {
        val result = linkedMapOf<String, DirectoryEntry>()
        var offset = 0
        while (offset + 128 <= data.size) {
            val nameLength = u16(data, offset + 0x40)
            if (nameLength >= 2) {
                val name = String(data, offset, nameLength.toInt() - 2, StandardCharsets.UTF_16LE)
                val entry = DirectoryEntry(
                    name = name,
                    type = data[offset + 0x42].toInt() and 0xFF,
                    startSector = u32(data, offset + 0x74),
                    size = u64(data, offset + 0x78)
                )
                result[name] = entry
            }
            offset += 128
        }
        return result
    }

    private fun readRegularChain(startSector: Long, requestedSize: Long): ByteArray {
        if (requestedSize <= 0 || startSector == END || startSector == FREE) return ByteArray(0)
        val maxSize = min(requestedSize, bytes.size.toLong()).toInt()
        val output = ByteArrayOutputStream(maxSize)
        var sector = startSector
        val visited = HashSet<Long>()
        while (sector != END && sector != FREE && output.size() < maxSize) {
            require(visited.add(sector)) { "XLS 扇区链存在循环" }
            require(sector <= Int.MAX_VALUE && sector.toInt() < fat.size) { "XLS 扇区索引越界" }
            val raw = readSector(sector)
            val count = min(sectorSize, maxSize - output.size())
            output.write(raw, 0, count)
            val next = fat[sector.toInt()]
            require(next != FAT_SECTOR && next != DIFAT_SECTOR) { "XLS 扇区链包含无效节点" }
            sector = next
        }
        return output.toByteArray()
    }

    private fun readMiniChain(startSector: Long, requestedSize: Long): ByteArray {
        val maxSize = min(requestedSize, miniStream.size.toLong()).toInt()
        val output = ByteArrayOutputStream(maxSize)
        var sector = startSector
        val visited = HashSet<Long>()
        while (sector != END && sector != FREE && output.size() < maxSize) {
            require(visited.add(sector)) { "XLS mini 扇区链存在循环" }
            require(sector <= Int.MAX_VALUE && sector.toInt() < miniFat.size) { "XLS mini 扇区索引越界" }
            val start = sector.toInt() * miniSectorSize
            require(start in 0..miniStream.size && start + miniSectorSize <= miniStream.size) { "XLS mini 扇区越界" }
            val count = min(miniSectorSize, maxSize - output.size())
            output.write(miniStream, start, count)
            sector = miniFat[sector.toInt()]
        }
        return output.toByteArray()
    }

    private fun readSector(sector: Long): ByteArray {
        require(sector <= Int.MAX_VALUE) { "XLS 扇区索引过大" }
        val offset = HEADER_SIZE.toLong() + sector * sectorSize
        require(offset >= HEADER_SIZE && offset + sectorSize <= bytes.size) { "XLS 扇区超出文件范围" }
        return bytes.copyOfRange(offset.toInt(), offset.toInt() + sectorSize)
    }

    private data class DirectoryEntry(
        val name: String,
        val type: Int,
        val startSector: Long,
        val size: Long
    )
}

private object BiffWorkbook {
    private const val BOF = 0x0809L
    private const val EOF = 0x000AL
    private const val SST = 0x00FCL
    private const val LABEL_SST = 0x00FDL
    private const val LABEL = 0x0204L
    private const val NUMBER = 0x0203L
    private const val RK = 0x027EL
    private const val MUL_RK = 0x00BDL

    fun readFirstSheet(data: ByteArray): List<List<String>> {
        val sharedStrings = readSharedStrings(data)
        val cells = linkedMapOf<Pair<Int, Int>, String>()
        var inFirstSheet = false
        var foundSheet = false
        var position = 0
        while (position + 4 <= data.size) {
            val type = u16(data, position)
            val length = u16(data, position + 2).toInt()
            val end = position + 4 + length
            if (end > data.size) break

            when (type) {
                BOF -> {
                    val streamType = if (length >= 6) u16(data, position + 6) else -1L
                    if (streamType == 0x0010L) {
                        if (!foundSheet) {
                            foundSheet = true
                            inFirstSheet = true
                        } else {
                            inFirstSheet = false
                        }
                    }
                }
                EOF -> if (inFirstSheet) inFirstSheet = false
                LABEL_SST -> if (inFirstSheet && length >= 10) {
                    val row = u16(data, position + 4).toInt()
                    val column = u16(data, position + 6).toInt()
                    val stringIndex = u32(data, position + 10).toInt()
                    val value = sharedStrings.getOrNull(stringIndex).orEmpty()
                    if (value.isNotEmpty()) cells[row to column] = value
                }
                LABEL -> if (inFirstSheet && length >= 8) {
                    val row = u16(data, position + 4).toInt()
                    val column = u16(data, position + 6).toInt()
                    readUnicodeString(data, position + 10, end)?.text?.let { cells[row to column] = it }
                }
                NUMBER -> if (inFirstSheet && length >= 14) {
                    val row = u16(data, position + 4).toInt()
                    val column = u16(data, position + 6).toInt()
                    val value = java.lang.Double.longBitsToDouble(u64(data, position + 10))
                    cells[row to column] = formatNumber(value)
                }
                RK -> if (inFirstSheet && length >= 10) {
                    val row = u16(data, position + 4).toInt()
                    val column = u16(data, position + 6).toInt()
                    cells[row to column] = formatRk(u32(data, position + 10))
                }
                MUL_RK -> if (inFirstSheet && length >= 10) {
                    val row = u16(data, position + 4).toInt()
                    val firstColumn = u16(data, position + 6).toInt()
                    val lastColumn = u16(data, end - 2).toInt()
                    val count = lastColumn - firstColumn + 1
                    repeat(count) { index ->
                        val rkOffset = position + 8 + index * 6
                        if (rkOffset + 6 <= end) cells[row to (firstColumn + index)] = formatRk(u32(data, rkOffset + 2))
                    }
                }
            }
            position = end
        }

        if (cells.isEmpty()) error("XLS 第一个工作表没有可读取的单元格")
        val maxRow = cells.keys.maxOf { it.first }
        val maxColumn = cells.keys.maxOf { it.second }
        return (0..maxRow).map { row ->
            (0..maxColumn).map { column -> cells[row to column].orEmpty() }
        }
    }

    private fun readSharedStrings(data: ByteArray): List<String> {
        var position = 0
        while (position + 4 <= data.size) {
            val type = u16(data, position)
            val length = u16(data, position + 2).toInt()
            val end = position + 4 + length
            if (end > data.size) break
            if (type == SST && length >= 8) {
                val uniqueCount = u32(data, position + 8).toInt()
                var cursor = position + 12
                return buildList {
                    repeat(uniqueCount) {
                        val value = readUnicodeString(data, cursor, end) ?: return@buildList
                        add(value.text)
                        cursor = value.nextOffset
                    }
                }
            }
            position = end
        }
        return emptyList()
    }

    private fun formatRk(raw: Long): String {
        val value = if ((raw and 0x2L) != 0L) {
            (raw shr 2).toInt().toString()
        } else {
            val bits = (raw and 0xFFFFFFFCL) shl 32
            java.lang.Double.longBitsToDouble(bits).toString()
        }
        return if ((raw and 0x1L) != 0L) {
            val number = value.toDoubleOrNull() ?: return value
            (number / 100.0).toString()
        } else value
    }

    private fun formatNumber(value: Double): String {
        return if (value % 1.0 == 0.0) value.toLong().toString() else value.toString()
    }

    private data class ParsedString(val text: String, val nextOffset: Int)

    private fun readUnicodeString(data: ByteArray, start: Int, limit: Int): ParsedString? {
        if (start + 3 > limit) return null
        val characterCount = u16(data, start).toInt()
        val flags = data[start + 2].toInt() and 0xFF
        val is16Bit = (flags and 0x01) != 0
        val byteCount = characterCount * if (is16Bit) 2 else 1
        val textStart = start + 3
        val textEnd = textStart + byteCount
        if (textEnd > limit) return null
        val text = if (is16Bit) {
            String(data, textStart, byteCount, StandardCharsets.UTF_16LE)
        } else {
            String(data, textStart, byteCount, windows1252)
        }
        var next = textEnd
        if ((flags and 0x08) != 0) {
            if (next + 2 > limit) return null
            val richTextRuns = u16(data, next).toInt()
            next += 2 + richTextRuns * 4
        }
        if ((flags and 0x04) != 0) {
            if (next + 4 > limit) return null
            val extendedLength = u32(data, next).toInt()
            next += 4 + extendedLength
        }
        return if (next <= limit) ParsedString(text, next) else null
    }

    private val windows1252: Charset = Charset.forName("windows-1252")
}

private fun u16(data: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 2 <= data.size) { "XLS 数据偏移越界" }
    return (data[offset].toLong() and 0xFF) or ((data[offset + 1].toLong() and 0xFF) shl 8)
}

private fun u32(data: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 4 <= data.size) { "XLS 数据偏移越界" }
    var value = 0L
    repeat(4) { index -> value = value or ((data[offset + index].toLong() and 0xFF) shl (8 * index)) }
    return value
}

private fun u64(data: ByteArray, offset: Int): Long {
    require(offset >= 0 && offset + 8 <= data.size) { "XLS 数据偏移越界" }
    var value = 0L
    repeat(8) { index -> value = value or ((data[offset + index].toLong() and 0xFF) shl (8 * index)) }
    return value
}
